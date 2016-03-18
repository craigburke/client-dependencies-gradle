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
package com.craigburke.gradle.client.registry.npm

import static com.craigburke.gradle.client.registry.core.ResolverUtil.withLock
import static com.craigburke.gradle.client.registry.npm.NpmUtil.extractTarball
import static groovyx.gpars.GParsPool.withExistingPool

import com.craigburke.gradle.client.registry.core.CircularDependencyException
import com.craigburke.gradle.client.registry.core.DependencyResolveException
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.RegistryBase

import org.gradle.api.logging.Logger
import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonSlurper

/**
 *
 * Registry to resolves NPM Dependencies
 *
 * @author Craig Burke
 */
class NpmRegistry extends RegistryBase implements Registry {

    static final String DEFAULT_URL = 'https://registry.npmjs.org'

    NpmRegistry(String url, Logger log) {
        super(url, log, [NpmResolver])
    }

    Dependency loadDependency(Dependency declaredDependency, Dependency parent) {
        log.info "Loading dependency: ${declaredDependency}"

        Dependency dependency = declaredDependency.clone()
        dependency.sourceFolder = new File("${cachePath}/${dependency.name}/source/")
        dependency.parent = parent
        dependency.registry = this

        if (declaredDependency.url) {
            dependency.version = Version.parse(declaredDependency.versionExpression)
        }
        else {
            dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, getVersionList(dependency))
        }

        if (!dependency.version) {
            String exceptionMessage = "Couldn't resolve ${dependency.name}@${dependency.versionExpression}"
            throw new DependencyResolveException(exceptionMessage)
        }

        boolean downloadedFromCache = (checkGlobalCache && downloadDependencyFromCache(dependency))

        if (!downloadedFromCache) {
            getResolver(dependency).downloadDependency(dependency)
        }

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

    private Map<String, String> getDependencies(Dependency dependency) {
        File packageJson = new File("${dependency.sourceFolder.absolutePath}/package.json")

        if (packageJson.exists()) {
            def json = new JsonSlurper().parse(packageJson)
            (json.dependencies ?: [:]) + (json.peerDependencies ?: [:])
        }
        else {
            [:]
        }
    }

    private List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclude) {
        withExistingPool(RegistryBase.pool) {
            getDependencies(dependency)
                    .findAll { String name, String childVersion -> !exclude.contains(name) }
                    .collectParallel { String name, String versionExpression ->
                        if (dependency.ancestorsAndSelf*.name.contains(name)) {
                            String exceptionMessage = "Circular dependency created by dependency ${name}@${versionExpression}"
                            throw new CircularDependencyException(exceptionMessage)
                        }

                        Dependency child = new Dependency(
                                name: name,
                                versionExpression: versionExpression,
                                exclude: exclude
                        )

                        loadDependency(child, dependency)
                    } ?: []
        } as List<Dependency>
    }

    boolean downloadDependencyFromCache(Dependency dependency) {
        withLock(dependency.key) {
            String npmCachePath = "${System.getProperty('user.home')}/.npm"
            String cacheFilePath = "${npmCachePath}/${dependency.name}/${dependency.version.fullVersion}/package.tgz"
            File cacheFile = new File(cacheFilePath)

            if (cacheFile.exists()) {
                log.info "Loading ${dependency} from ${cacheFilePath}"
                extractTarball(cacheFile, dependency.sourceFolder)
                true
            }
            else {
                false
            }
        }
    }
}
