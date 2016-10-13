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

import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.AbstractRegistry
import com.craigburke.gradle.client.dependency.Dependency
import groovy.json.JsonSlurper

/**
 *
 * Registry to resolves Bower Dependencies
 *
 * @author Craig Burke
 */
class BowerRegistry extends AbstractRegistry implements Registry {

    static final String DEFAULT_BOWER_URL = 'https://bower.herokuapp.com'
    static final List<String> DEFAULT_BOWER_FILENAMES = ['bower.json', '.bower.json']

    BowerRegistry(String name, String url = DEFAULT_BOWER_URL, List<String> configFilenames = DEFAULT_BOWER_FILENAMES) {
        super(name, url, configFilenames, [GithubResolver, GitResolver])
    }

    @Override
    List<SimpleDependency> getChildDependencies(Dependency dependency) {
        File bowerConfigFile = getConfigFile(dependency.sourceDir)
        def bowerConfigJson = new JsonSlurper().parse(bowerConfigFile)

        bowerConfigJson.dependencies?.collect { String name, String versionExpression ->
            new SimpleDependency(name: name, versionExpression: versionExpression)
        } ?: []
    }

    @Override
    boolean loadSourceFromGlobalCache(Dependency dependency) {
        String bowerCachePath = "${globalCacheDir.absolutePath}/packages"
        String cachePath = "${bowerCachePath}/${getMD5Hash(dependency.fullUrl)}/${dependency.version.fullVersion}/"
        File cacheFolder = new File(cachePath)

        if (cacheFolder.exists()) {
            log.info "Loading ${dependency} from ${cachePath}"
            AntBuilder builder = new AntBuilder()
            builder.project.buildListeners.first().setMessageOutputLevel(0)
            builder.copy(todir: dependency.sourceDir.absolutePath) {
                fileset(dir: cacheFolder.absolutePath)
            }
            true
        }
        else {
            false
        }
    }

    @Override
    Map loadInfoFromGlobalCache(Dependency dependency) {
        File cacheRoot = new File("${globalCacheDir.absolutePath}/registry/bower.herokuapp.com/lookup")
        File cacheFile = cacheRoot.listFiles()
                .findAll { File file -> !file.directory }
                .find { File file -> file.name.startsWith("${dependency.name}_") }
        if (cacheFile) {
            new JsonSlurper().parse(cacheFile) as Map
        }
        else {
            null
        }
    }

    @Override
    Map loadInfoFromRegistry(Dependency dependency) {
        URL url = new URL("${dependency.registry.url}/packages/${dependency.name}")
        String jsonText = url.getText(requestProperties: ['User-Agent': DEFAULT_USER_AGENT])
        new JsonSlurper().parseText(jsonText) as Map
    }

}
