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
import com.craigburke.gradle.client.registry.core.RegistryUtil
import groovy.transform.CompileStatic
import java.util.regex.Pattern

/**
 * dependency
 * @author Craig Burke
 */
@CompileStatic
class Dependency extends SimpleDependency implements Cloneable {

    static final String GITHUB_URL_PREFIX = 'https://github.com'
    static final Pattern GITHUB_PROJECT_PATTERN = ~/^[^\/]*\/[^\/]*$/

    Registry registry
    String from
    String into

    Version version
    Dependency parent

    List<Dependency> children = []
    List<String> exclude = []

    boolean transitive = true
    Closure copyConfig

    Map info = [:]
    File baseSourceDir

    void setExclude(Object exclude) {
        if (exclude instanceof String) {
            this.exclude = [exclude as String]
        }
        else {
            this.exclude = exclude as List<String>
        }
    }

    File getSourceDir() {
        new File("${baseSourceDir.absolutePath}/${RegistryUtil.getMD5Hash(versionExpression)}/")
    }

    void setChildren(List<Dependency> children) {
        children*.parent = this
        this.children = children
    }

    static List<Dependency> flattenList(List<Dependency> dependencies) {
        List<Dependency> children = dependencies
                .findAll { it.children }
                .collectMany { flattenList(it.children) } as List<Dependency>

        (dependencies + children) as List<Dependency>
    }

    List<Dependency> getAncestorsAndSelf() {
        collectAncestors(this)
    }

    private static List<Dependency> collectAncestors(Dependency dependency) {
        if (dependency.parent) {
            ([dependency] + collectAncestors(dependency.parent)) as List<Dependency>
        }
        else {
            [dependency]
        }
    }

    String getFullUrl() {
        String urlValue = url ?: info?.url

        if (urlValue ==~ GITHUB_PROJECT_PATTERN) {
            "${GITHUB_URL_PREFIX}/${urlValue}"
        }
        else {
            urlValue
        }
    }

    String getKey() {
        "${name}@${version?.fullVersion ?: versionExpression}"
    }

    String getDestinationPath() {
        into ?: formatFolderName(name)
    }

    private String formatFolderName(String name) {
        name.replaceAll(/\s+/, '-')
    }

    String getReleaseFolder(List<String> releaseFolders) {
        if (from != null) {
            return from
        }

        File[] sourceFolders = sourceDir?.listFiles()?.findAll { File file -> file.directory } ?: []
        List<String> folderNames = sourceFolders*.name
        releaseFolders?.find { folderNames.contains(it) } ?: ''
    }

    Object clone() {
        Dependency result = new Dependency()
        Dependency source = this

        result.with {
            name = source.name
            transitive = source.transitive
            url = source.url
            versionExpression = source.versionExpression
            from = source.from
            into = source.into
            exclude = source.exclude
            parent = source.parent?.clone() as Dependency
            copyConfig = source.copyConfig?.clone() as Closure
            baseSourceDir = source.baseSourceDir ? new File(source.baseSourceDir.absolutePath) : null
            version = source.version?.clone() as Version
        }

        result
    }

    String toString() {
        key
    }
}
