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

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.transform.CompileStatic
import jsr166y.ForkJoinPool
import org.gradle.api.logging.Logger

/**
 *
 * Base Registry class
 *
 * @author Craig Burke
 */
@CompileStatic
abstract class RegistryBase implements Registry {

    static final String DEFAULT_PATH_SEPARATOR = '/'

    String url
    String cachePath
    String installPath
    boolean useGlobalCache
    boolean checkDownloads

    static ForkJoinPool pool
    protected Logger log
    protected List<Resolver> resolvers

    RegistryBase(String url, Logger log, List<Class<Resolver>> resolvers) {
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
        formatPath("${cachePath}/${dependencyName}")
    }

    List<Version> getVersionList(Dependency dependency) {
        getResolver(dependency).getVersionList(dependency)
    }

    Resolver getResolver(Dependency dependency) {
        resolvers.find { it.canResolve(dependency) }
    }

    Dependency loadDependency(Dependency declaredDependency, Dependency parent) {
        log.info "Loading dependency: ${declaredDependency}"

        Dependency dependency = declaredDependency.clone()
        dependency.registry = this
        dependency.parent = parent
        dependency.sourceFolder = new File("${cachePath}/${dependency.name}/source/")

        if (declaredDependency.url) {
            dependency.version = Version.parse(declaredDependency.versionExpression)
        }
        else {
            dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, getVersionList(dependency))
        }

        dependency.url = getDependencyUrl(dependency)

        if (!dependency.version) {
            String exceptionMessage = "Couldn't resolve ${dependency.name}@${dependency.versionExpression}"
            throw new DependencyResolveException(exceptionMessage)
        }

        boolean downloadedFromCache = (useGlobalCache && downloadDependencyFromCache(dependency))

        if (!downloadedFromCache) {
            getResolver(dependency).downloadDependency(dependency)
        }

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

    abstract String getDependencyUrl(Dependency dependency)
    abstract boolean downloadDependencyFromCache(Dependency dependency)
    abstract List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions)
}