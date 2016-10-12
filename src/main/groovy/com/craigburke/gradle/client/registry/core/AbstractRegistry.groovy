/*
 * Copyright 2016 Craig Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craigburke.gradle.client.registry.core

import static com.craigburke.gradle.client.registry.core.RegistryUtil.withLock
import static groovyx.gpars.GParsPool.withExistingPool

import com.craigburke.gradle.client.plugin.ClientDependenciesPlugin
import org.gradle.api.logging.Logging
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import jsr166y.ForkJoinPool
import org.gradle.api.logging.Logger

/**
 *
 * Abstract Registry class
 *
 * @author Craig Burke
 */
abstract class AbstractRegistry implements Registry {
    static final String DEFAULT_PATH_SEPARATOR = '/'

    String name
    String url
    boolean offline
    File localCacheDir
    File globalCacheDir
    File installDir

    boolean useGlobalCache
    boolean checkDownloads

    static ForkJoinPool pool
    protected Logger log
    protected List<Resolver> resolvers
    protected List<String> configFilenames

    protected AbstractRegistry(String name,
                               String url,
                               List<String> configFilenames,
                               List<Class<Resolver>> resolvers) {
        this.name = name
        this.url = url
        this.log = Logging.getLogger(ClientDependenciesPlugin)
        this.resolvers = resolvers.collect { it.newInstance(log) as Resolver }
        this.configFilenames = configFilenames
    }

    static void setThreadPoolSize(int poolSize) {
        pool = new ForkJoinPool(poolSize)
    }

    protected static String formatPath(String path) {
        path.replace('\\', DEFAULT_PATH_SEPARATOR).replace('//', DEFAULT_PATH_SEPARATOR)
    }

    protected String getMainFolderPath(String dependencyName) {
        formatPath("${localCacheDir.absolutePath}/${dependencyName}")
    }

    @Override
    Resolver getResolver(Dependency dependency) {
        resolvers.find { it.canResolve(dependency) }
    }

    @Override
    Dependency loadDependency(Dependency declaredDependency, Dependency parent) {
        log.info "Loading dependency: ${declaredDependency}"

        Dependency dependency = declaredDependency.clone() as Dependency
        dependency.registry = this
        dependency.parent = parent
        dependency.baseSourceDir = new File("${localCacheDir.absolutePath}/${dependency.name}/")
        setInfo(dependency)
        Resolver resolver = getResolver(dependency)

        if (declaredDependency.fullUrl) {
            dependency.version = Version.parse(declaredDependency.versionExpression)
        }
        else {
            List<Version> versionList = resolver.getVersionList(dependency)
            dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, versionList)
        }

        loadSource(dependency)
        dependency.version = dependency.version ?: getVersionFromSource(dependency.sourceDir)

        if (!dependency.version) {
            String exceptionMessage = "Couldn't resolve ${dependency.name}@${dependency.versionExpression}"
            throw new DependencyResolveException(exceptionMessage)
        }

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

    protected void loadSource(Dependency dependency) {
        withLock("${name}:${dependency.key}") {
            dependency.sourceDir.mkdirs()

            if (useGlobalCache) {
                loadSourceFromGlobalCache(dependency)
            }

            Resolver resolver = getResolver(dependency)
            resolver.resolve(dependency)
        }
    }

    protected static File getInfoFile(Dependency dependency) {
        new File("${dependency.baseSourceDir.absolutePath}/info.json")
    }

    protected void setInfo(Dependency dependency) {
        withLock("${name}:${dependency.name}") {
            dependency.info = loadInfoFromLocalCache(dependency)
            boolean loadedFromLocalCache = dependency.info as boolean

            if (!loadedFromLocalCache && offline && dependency.registry.useGlobalCache) {
                dependency.info = loadInfoFromGlobalCache(dependency)
            }

            if (!dependency.info && !dependency.fullUrl) {
                dependency.info = loadInfoFromRegistry(dependency)
            }

            if (!loadedFromLocalCache && dependency.info) {
                File infoFile = getInfoFile(dependency)
                infoFile.parentFile.mkdirs()
                infoFile.text = new JsonBuilder(dependency.info).toPrettyString()
            }

            Resolver resolver = getResolver(dependency)
            resolver.afterInfoLoad(dependency)
        }
    }

    protected Map loadInfoFromLocalCache(Dependency dependency) {
        File infoFile = getInfoFile(dependency)
        if (infoFile.exists() && !infoFile.directory) {
            new JsonSlurper().parse(infoFile) as Map
        }
        else {
            null
        }
    }

    protected List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) {
        withExistingPool(pool) {
            getChildDependencies(dependency)
                .findAll { SimpleDependency child -> !exclusions.contains(child.name) }
                .collectParallel { SimpleDependency child ->
                    if (dependency.ancestorsAndSelf*.name.contains(child.name)) {
                        null
                    }
                    else {
                        Dependency childDependency = new Dependency(
                                name: child.name,
                                versionExpression: child.versionExpression,
                                exclude: exclusions
                        )
                        loadDependency(childDependency, dependency)
                    }
            }
            .findAllParallel { it != null }
        } as List<Dependency>
    }

    File getConfigFile(File sourceDir) {
        configFilenames
                .collect { String filename -> new File("${sourceDir.absolutePath}/${filename}") }
                .find { it.exists() }
    }

    Version getVersionFromSource(File sourceDir) {
        File configFile = getConfigFile(sourceDir)
        if (configFile) {
            Map config = new JsonSlurper().parse(configFile) as Map
            Version.parse(config.version as String)
        }
        else {
            null
        }
    }

    abstract boolean loadSourceFromGlobalCache(Dependency dependency)
    abstract Map loadInfoFromGlobalCache(Dependency dependency)
    abstract Map loadInfoFromRegistry(Dependency dependency)
    abstract List<SimpleDependency> getChildDependencies(Dependency dependency)
}
