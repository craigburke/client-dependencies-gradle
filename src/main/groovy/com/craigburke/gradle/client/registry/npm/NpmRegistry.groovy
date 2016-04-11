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

import static com.craigburke.gradle.client.registry.npm.NpmUtil.extractTarball

import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.AbstractRegistry

import org.gradle.api.logging.Logger
import com.craigburke.gradle.client.dependency.Dependency
import groovy.json.JsonSlurper

/**
 *
 * Registry to resolves NPM Dependencies
 *
 * @author Craig Burke
 */
class NpmRegistry extends AbstractRegistry implements Registry {

    static final String DEFAULT_URL = 'https://registry.npmjs.org'

    NpmRegistry(String url, Logger log) {
        super(url, log, [NpmResolver])
    }

    List<SimpleDependency> getChildDependencies(Dependency dependency) {
        File packageJson = new File("${dependency.sourceDir.absolutePath}/package.json")

        if (packageJson.exists()) {
            def json = new JsonSlurper().parse(packageJson)
            ((json.dependencies ?: [:]) + (json.peerDependencies ?: [:])).collect { String name, String version ->
                new SimpleDependency(name: name, versionExpression: version)
            }
        }
        else {
            []
        }

    }

    boolean downloadDependencyFromCache(Dependency dependency) {
        String npmCachePath = "${System.getProperty('user.home')}/.npm"
        String cacheFilePath = "${npmCachePath}/${dependency.name}/${dependency.version.fullVersion}/package.tgz"
        File cacheFile = new File(cacheFilePath)

        if (cacheFile.exists()) {
            log.info "Loading ${dependency} from ${cacheFilePath}"
            extractTarball(cacheFile, dependency.sourceDir)
            true
        }
        else {
            false
        }

    }

    Map loadInfoFromRegistry(Dependency dependency) {
        log.info "Loading info for ${dependency} from ${dependency.registry.url}"
        boolean isScoped = dependency.name.startsWith('@')
        String name = isScoped ? dependency.name[1..-1] : dependency.name
        String encodedName = "${isScoped ? '@' : ''}${URLEncoder.encode(name, 'UTF-8')}"
        URL url = new URL("${dependency.registry.url}/${encodedName}")
        new JsonSlurper().parse(url) as Map
    }

    Map loadInfoFromGlobalCache(Dependency dependency) {
        String folderName = dependency.name.replaceAll('@', '_40').replaceAll('/', '_252')
        File cacheFile = new File("${System.getProperty('user.home')}/.npm/${folderName}/.cache.json")
        if (cacheFile.exists()) {
            log.info "Loading info for ${dependency} from ${cacheFile.absolutePath}"
            new JsonSlurper().parse(cacheFile) as Map
        }
        else {
            null
        }
    }
}
