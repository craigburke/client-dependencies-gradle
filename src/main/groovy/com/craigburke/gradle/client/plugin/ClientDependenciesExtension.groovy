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
package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.bower.BowerRegistry
import com.craigburke.gradle.client.registry.npm.NpmRegistry
import com.craigburke.gradle.client.registry.core.Registry
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 *
 * Extension for client dependencies
 *
 * @author Craig Burke
 */
class ClientDependenciesExtension {

    Project project

    int threadPoolSize = 15

    String installDir
    String cacheDir

    boolean useGlobalCache = true
    boolean checkDownloads = true

    List<String> fileExtensions = ['css', 'js', 'eot', 'svg', 'ttf', 'woff', 'woff2', 'ts']
    List<String> releaseFolders = ['dist', 'release']
    List<String> copyIncludes = []
    List<String> copyExcludes = ['**/*.min.js', '**/*.min.css', '**/*.map', '**/Gruntfile.js',
                                 'index.js', 'gulpfile.js', 'source/**']

    Closure defaultCopy

    ClientDependenciesExtension(Project project) {
        this.project = project
    }

    Map<String, Registry> registryMap = [:]
    List<Dependency> rootDependencies = []

    def methodMissing(String registryName, args) {
       if (args && args.last() instanceof Closure) {
           Registry registry = registryMap[registryName]
           DependencyBuilder dependencyBuilder = new DependencyBuilder(registry)
           Closure clonedClosure = args.last().rehydrate(dependencyBuilder, this, this)
           clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
           clonedClosure()
           rootDependencies += dependencyBuilder.rootDependencies
       }
    }

    void registry(Map props) {
        String url = props.url as String
        Logger log = project.logger
        Registry registry = (props.type == 'bower' ? new BowerRegistry(url, log) : new NpmRegistry(url, log))
        registryMap[props.name as String] = registry
    }

    Closure getDefaultCopyConfig() {
        if (defaultCopy) {
            return defaultCopy
        }

        List<String> includes = fileExtensions.collect { "**/*.${it}" } + copyIncludes

        return {
            exclude copyExcludes
            include includes
        }
    }
}
