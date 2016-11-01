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
import com.craigburke.gradle.client.registry.npm.YarnRegistry
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.CollectionUtils

/**
 *
 * Extension for client dependencies
 *
 * @author Craig Burke
 */
class ClientDependenciesExtension {

    Project project

    int threadPoolSize = 15
    List<Registry> registries = []
    List<Dependency> rootDependencies = []

    boolean useGlobalCache = true
    boolean checkDownloads = true

    String githubUsername
    String githubPassword

    private Object installDir
    private Object cacheDir

    private final List<Object> fileExtensions = ['css', 'js', 'eot', 'svg', 'ttf', 'woff', 'woff2', 'ts',
                                                 'jpg', 'jpeg', 'png', 'gif']

    private final List<Object> releaseFolders = ['dist', 'release']

    private final List<Object> copyIncludes = []
    private final List<Object> copyExcludes = ['**/*.min.js', '**/*.min.css', '**/*.map', '**/Gruntfile.js',
                                 'gulpfile.js', 'source/**']

    Closure defaultCopy

    ClientDependenciesExtension(Project project) {
        this.project = project
    }

    void setFileExtensions(Object... extensions) {
        this.fileExtensions.clear()
        this.fileExtensions.addAll(extensions)
    }

    void fileExtensions(Object... extensions) {
        this.fileExtensions.addAll(extensions)
    }

    List<String> getFileExtensions() {
        CollectionUtils.stringize(this.fileExtensions)
    }

    void setReleaseFolders(Object... extensions) {
        this.releaseFolders.clear()
        this.releaseFolders.addAll(extensions)
    }

    void releaseFolders(Object... extensions) {
        this.releaseFolders.addAll(extensions)
    }

    List<String> getCopyIncludes() {
        CollectionUtils.stringize(this.copyIncludes)
    }

    void setCopyIncludes(Object... includes) {
        this.copyIncludes.clear()
        this.copyIncludes.addAll(includes)
    }

    void copyIncludes(Object... includes) {
        this.copyIncludes.addAll(includes)
    }

    List<String> getCopyExcludes() {
        CollectionUtils.stringize(this.copyExcludes)
    }

    void setCopyExcludes(Object... includes) {
        this.copyExcludes.clear()
        this.copyExcludes.addAll(includes)
    }

    void copyExcludes(Object... includes) {
        this.copyExcludes.addAll(includes)
    }

    List<String> getReleaseFolders() {
        CollectionUtils.stringize(this.releaseFolders)
    }

    void setInstallDir(Object installDir) {
        this.installDir = installDir
    }

    File getInstallDir() {
        this.installDir ? project.file(this.installDir) : null
    }

    void setCacheDir(Object cacheDir) {
        this.cacheDir = cacheDir
    }

    File getCacheDir() {
        this.cacheDir ? project.file(this.cacheDir) : null
    }

    def methodMissing(String registryName, args) {
       if (args && args.last() instanceof Closure) {
           Registry registry = findRegistry(registryName)
           DependencyBuilder dependencyBuilder = new DependencyBuilder(registry)
           Closure clonedClosure = args.last().rehydrate(dependencyBuilder, this, this)
           clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
           clonedClosure()
           rootDependencies += dependencyBuilder.rootDependencies
       }
    }

    void registry(Map props = [:], String name) {
        String url = props.url as String
        Registry registry
        switch (props.type) {
            case 'bower':
                registry = new BowerRegistry(name, url)
                break
            case 'npm':
                registry = new NpmRegistry(name, url)
                break
            case 'yarn':
                registry = new YarnRegistry(name, url)
                break
            default:
                throw new GradleException("Unknown Registry: $name")
        }
        registry.githubUsername = githubUsername
        registry.githubPassword = githubPassword
        registries += registry
    }

    Registry findRegistry(String name) {
        registries.find { it.name == name  }
    }

    Closure getCopyConfig() {
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
