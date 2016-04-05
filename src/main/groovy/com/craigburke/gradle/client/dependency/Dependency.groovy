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
package com.craigburke.gradle.client.dependency

import com.craigburke.gradle.client.registry.core.Registry
import groovy.transform.AutoClone
import groovy.transform.CompileStatic

/**
 * dependency
 * @author Craig Burke
 */
@CompileStatic
@AutoClone
class Dependency {

    Registry registry
    String name
    String versionExpression
    String url
    String from
    String into

    Version version
    Dependency parent

    List<Dependency> children = []
    List<String> exclude = []

    File sourceFolder

    boolean transitive = true
    Closure copyConfig

    void setExclude(String exclude) {
        this.exclude = [exclude]
    }

    void setExclude(List<String> exclude) {
        this.exclude = exclude
    }

    void setChildren(List<Dependency> children) {
        children*.parent = this
        this.children = children
    }

    static List<Dependency> flattenList(List<Dependency> dependencies) {
        List<Dependency> children = dependencies
                .findAll { it.children }
                .collectMany { flattenList(it.children) } as List<Dependency>

        dependencies + children
    }

    List<Dependency> getAncestorsAndSelf() {
        collectAncestors(this)
    }

    private static List<Dependency> collectAncestors(Dependency dependency) {
        if (dependency.parent) {
            [dependency] + collectAncestors(dependency.parent)
        }
        else {
            [dependency]
        }
    }

    String getKey() {
        "${name}@${version?.fullVersion ?: versionExpression}"
    }

    String getDestinationPath() {
        into ?: formatFolderName(name)
    }

    private String formatFolderName(String name) {
        name.replaceAll('@', '').replaceAll(/[\s+|\/]/, '-')
    }

    String getReleaseFolder(List<String> releaseFolders) {
        if (from) {
            return from
        }

        File[] sourceFolders = sourceFolder?.listFiles()?.findAll { File file -> file.directory }
        List<String> folderNames = sourceFolders*.name
        releaseFolders?.find { folderNames.contains(it) } ?: ''
    }

    String toString() {
        key
    }
}
