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

    String url

    File localCacheDir
    File globalCacheDir
    File installDir

    boolean useGlobalCache
    boolean checkDownloads

    static ForkJoinPool pool
    protected Logger log
    protected List<Resolver> resolvers

    protected AbstractRegistry(String url, Logger log, List<Class<Resolver>> resolvers) {
        this.url = url
        this.log = log
        this.resolvers = resolvers.collect { it.newInstance(log) as Resolver }
    }

    static void setThreadPoolSize(int poolSize) {
        pool = new ForkJoinPool(poolSize)
    }

    static String formatPath(String path) {
        path.replace('\\', DEFAULT_PATH_SEPARATOR).replace('//', DEFAULT_PATH_SEPARATOR)
    }

    String getMainFolderPath(String dependencyName) {
        formatPath("${localCacheDir.absolutePath}/${dependencyName}")
    }

    Resolver getResolver(Dependency dependency) {
        resolvers.find { it.canResolve(dependency) }
    }

    Dependency loadDependency(Dependency declaredDependency, Dependency parent) {
        log.info "Loading dependency: ${declaredDependency}"

        Dependency dependency = declaredDependency.clone() as Dependency
        dependency.registry = this
        dependency.parent = parent
        dependency.baseSourceDir = new File("${localCacheDir.absolutePath}/${dependency.name}/")
        dependency.info = loadInfo(dependency)

        if (declaredDependency.url) {
            dependency.version = Version.parse(declaredDependency.versionExpression)
        }
        else {
            List<Version> versionList = getResolver(dependency).getVersionList(dependency)
            dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, versionList)
        }

        dependency.url = dependency.url ?: dependency.info.url

        if (!dependency.version) {
            String exceptionMessage = "Couldn't resolve ${dependency.name}@${dependency.versionExpression}"
            throw new DependencyResolveException(exceptionMessage)
        }

        loadSource(dependency)

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

    void loadSource(Dependency dependency) {
        withLock(dependency.key) {
            Resolver resolver = getResolver(dependency)
            dependency.sourceDir.mkdirs()

            if (useGlobalCache) {
                loadSourceFromGlobalCache(dependency)
            }

            resolver.resolve(dependency)
        }
    }

    static File getInfoFile(Dependency dependency) {
        new File("${dependency.baseSourceDir.absolutePath}/info.json")
    }

    Map loadInfo(Dependency dependency) {
        withLock(dependency.name) {
            def info = loadInfoFromLocalCache(dependency)
            boolean loadedFromLocalCache = info as boolean

            if (!loadedFromLocalCache && dependency.registry.useGlobalCache) {
                info = loadInfoFromGlobalCache(dependency)
            }

            if (!info && !dependency.url) {
                info = loadInfoFromRegistry(dependency)
            }

            if (!loadedFromLocalCache && info) {
                File infoFile = getInfoFile(dependency)
                infoFile.parentFile.mkdirs()
                infoFile.text = new JsonBuilder(info).toPrettyString()
            }

            info ?: [:]
        } as Map
    }

    Map loadInfoFromLocalCache(Dependency dependency) {
        File infoFile = getInfoFile(dependency)
        if (infoFile.exists() && !infoFile.directory) {
            new JsonSlurper().parse(infoFile) as Map
        }
        else {
            null
        }
    }

    List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) {
        withExistingPool(pool) {
            getChildDependencies(dependency)
                .findAll { SimpleDependency child -> !exclusions.contains(child.name) }
                .collectParallel { SimpleDependency child ->
                    if (dependency.ancestorsAndSelf*.name.contains(child.name)) {
                        String message = "Circular dependency created by dependency ${child.name}@${child.versionExpression}"
                        throw new CircularDependencyException(message)
                    }

                    Dependency childDependency = new Dependency(name: child.name, versionExpression: child.versionExpression)
                    loadDependency(childDependency, dependency)
            }
        } as List<Dependency>
    }

    abstract boolean loadSourceFromGlobalCache(Dependency dependency)
    abstract Map loadInfoFromGlobalCache(Dependency dependency)
    abstract Map loadInfoFromRegistry(Dependency dependency)
    abstract List<SimpleDependency> getChildDependencies(Dependency dependency)
}
