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
package com.craigburke.gradle.client.registry.bower

import static com.craigburke.gradle.client.registry.core.RegistryUtil.getMD5Hash
import static com.craigburke.gradle.client.registry.core.RegistryUtil.withLock
import static groovyx.gpars.GParsPool.withExistingPool

import com.craigburke.gradle.client.registry.core.CircularDependencyException
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.AbstractRegistry
import org.gradle.api.logging.Logger
import com.craigburke.gradle.client.dependency.Dependency
import groovy.json.JsonSlurper

/**
 *
 * Registry to resolves Bower Dependencies
 *
 * @author Craig Burke
 */
class BowerRegistry extends AbstractRegistry implements Registry {

    static final String DEFAULT_URL = 'https://bower.herokuapp.com'

    BowerRegistry(String url, Logger log) {
        super(url, log, [GithubResolver, GitResolver])
    }

    List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) {
        File bowerConfigFile = new File("${dependency.sourceFolder.absolutePath}/bower.json")
        def bowerConfigJson = new JsonSlurper().parse(bowerConfigFile)
        withExistingPool(AbstractRegistry.pool) {
            bowerConfigJson.dependencies
                    .findAll { String name, String versionExpression -> !exclusions.contains(name) }
                    .collectParallel { String name, String versionExpression ->

                if (dependency.ancestorsAndSelf*.name.contains(name)) {
                    String message = "Circular dependency created by dependency ${name}@${versionExpression}"
                    throw new CircularDependencyException(message)
                }

                Dependency childDependency = new Dependency(name: name, versionExpression: versionExpression)
                loadDependency(childDependency, dependency)
            } ?: []
        } as List<Dependency>
    }

    String getDependencyUrl(Dependency dependency) {
        if (dependency.url) {
            return dependency.url
        }
        getInfo(dependency)?.url
    }

    boolean downloadDependencyFromCache(Dependency dependency) {
        withLock(dependency.key) {
            String bowerCachePath = "${System.getProperty('user.home')}/.cache/bower/packages"
            String cachePath = "${bowerCachePath}/${getMD5Hash(dependency.url)}/${dependency.version.fullVersion}/"
            File cacheFolder = new File(cachePath)

            if (cacheFolder.exists()) {
                log.info "Loading ${dependency} from ${cachePath}"
                AntBuilder builder = new AntBuilder()
                builder.project.buildListeners.first().setMessageOutputLevel(0)
                builder.copy(todir: dependency.sourceFolder.absolutePath) {
                    fileset(dir: cacheFolder.absolutePath)
                }
                true
            }
            else {
                false
            }
        }
    }

    Map getInfo(Dependency dependency) {
        URL url = new URL("${dependency.registry.url}/packages/${dependency.name}")
        new JsonSlurper().parse(url) as Map
    }

    Map loadInfoFromGlobalCache(Dependency dependency) {
        null
    }

    Map loadInfoFromRegistry(Dependency dependency) {
        null
    }

}
