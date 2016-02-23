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
package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit

import static groovyx.gpars.GParsPool.withExistingPool

class NpmRegistry extends RegistryBase implements Registry {

    NpmRegistry(String url = 'https://registry.npmjs.org') {
        super(url)
    }

    private String getSourceFolderPath(Dependency dependency) {
       "${getMainFolderPath(dependency.name)}/${dependency.version}/source/"
    }

    private getVersionListFromNpm(String dependencyName) {
        File mainConfigFile = new File("${getMainFolderPath(dependencyName)}/main.json")

        def versionListJson

        if (mainConfigFile.exists()) {
            versionListJson = new JsonSlurper().parse(mainConfigFile).versions
        } else {
            URL url = new URL("${this.url}/${dependencyName}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            versionListJson = json.versions
        }
        versionListJson
    }

    private String getDownloadUrlFromNpm(Dependency dependency) {
        getVersionListFromNpm(dependency.name)[dependency.version.fullVersion]?.dist?.tarball
    }

    Dependency loadDependency(DeclaredDependency declaredDependency) {
        String dependencyName = declaredDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)

        if (declaredDependency.url) {
            dependency.version = Version.parse(declaredDependency.versionExpression)
        }
        else {
            dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, getVersionList(declaredDependency))
        }

        if (!dependency.version) {
            throw new Exception("Couldn't resolve ${dependencyName}@${declaredDependency.versionExpression}")
        }

        dependency.downloadUrl = declaredDependency.url ?: getDownloadUrlFromNpm(dependency)
        downloadDependency(dependency)

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

    private Map<String, String> getDependencies(Dependency dependency) {
        File packageJson = new File("${getSourceFolderPath(dependency)}/package.json")

        if (packageJson.exists()) {
            def json = new JsonSlurper().parse(packageJson)
            json.dependencies
        }
        else {
            [:]
        }
    }

    private List<Dependency> loadChildDependencies(Dependency dependency, List<String> excludes) {
        withExistingPool(pool) {
            getDependencies(dependency)
                    .findAll { String name, String childVersion -> !excludes.contains(name)}
                    .collectParallel { String name, String childVersion ->
                DeclaredDependency childDependency = new DeclaredDependency(name: name, versionExpression: childVersion, exclude: excludes)
                loadDependency(childDependency)
            } ?: []
        } as List<Dependency>
    }

    List<Version> getVersionList(DeclaredDependency declaredDependency) {
        def versionListJson = getVersionListFromNpm(declaredDependency.name)
        versionListJson.collect { Version.parse(it.key as String) }
    }

    void downloadDependency(Dependency dependency) {
        File sourceFolder = new File("${getSourceFolderPath(dependency)}/")

        if (sourceFolder.exists()) {
            return
        }

        sourceFolder.mkdirs()

        if (dependency.downloadUrl.endsWith('tgz')) {
            File downloadFile = new File("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package.tgz")
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }

            AntBuilder builder = new AntBuilder()
            builder.project.buildListeners.first().setMessageOutputLevel(0)
            builder.untar(src: downloadFile.absolutePath, dest: sourceFolder.absolutePath, compression:'gzip', overwrite:true) {
                patternset {
                    include(name: 'package/**')
                }
                mapper {
                    globmapper(from: 'package/*', to:'*')
                }
            }

            downloadFile.delete()
        }
        else {
            Grgit.clone(dir: sourceFolder.absolutePath, uri: dependency.downloadUrl, refToCheckout: 'master')
        }
    }

    File getSourceFolder(Dependency dependency) {
        new File("${getSourceFolderPath(dependency)}")
    }

}
