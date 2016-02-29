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
import org.ajoberstar.grgit.operation.ResetOp

import static groovyx.gpars.GParsPool.withExistingPool

class BowerRegistry extends RegistryBase implements Registry {

    BowerRegistry(String url = 'https://bower.herokuapp.com') {
        super(url)
    }

    private getDependencyJson(String dependencyName) {
        File mainConfigFile = new File("${getMainFolderPath(dependencyName)}/main.json")

        def dependencyJson

        if (mainConfigFile.exists()) {
            dependencyJson = new JsonSlurper().parse(mainConfigFile)
        } else {
            URL url = new URL("${this.url}/packages/${dependencyName}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            dependencyJson = json
        }
        dependencyJson
    }

    private List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) {
        checkoutVersion(dependency.name, dependency.version.fullVersion)
        File bowerConfigFile = new File("${getRepoPath(dependency.name)}/bower.json")
        def bowerConfigJson = new JsonSlurper().parse(bowerConfigFile)
        withExistingPool(pool) {
            bowerConfigJson.dependencies
                    .findAll { String name, String versionExpression -> !exclusions.contains(name) }
                    .collectParallel { String name, String versionExpression ->
                        if (dependency.ancestorsAndSelf*.name.contains(name)) {
                            throw new CircularDependencyException("Circular dependency created by dependency ${name}@${versionExpression}")
                        }

                        DeclaredDependency childDependency = new DeclaredDependency(name: name, versionExpression: versionExpression)
                        loadDependency(childDependency, dependency)
                    } ?: []
        } as List<Dependency>
    }

    private File getRepoPath(String dependencyName) {
       new File("${getMainFolderPath(dependencyName)}/source/")
    }

    private void downloadRepository(DeclaredDependency dependency) {
        File repoPath = getRepoPath(dependency.name)

        if (!repoPath.exists()) {
            String gitUrl
            if (dependency.url) {
                gitUrl = dependency.url
            } else {
                def dependencyJson = getDependencyJson(dependency.name)
                gitUrl = dependencyJson.url
            }

            repoPath.mkdirs()
            Grgit.clone(dir: repoPath.absolutePath, uri: gitUrl, refToCheckout: 'master')
        }
    }

    private Grgit getRepository(String dependencyName) {
        File repoPath = getRepoPath(dependencyName)
        Grgit repo

        if (repoPath.exists()) {
            repo = Grgit.open(dir: repoPath.absolutePath)
        }

        repo
    }

    void checkoutVersion(String dependencyName, String version) {
        def repo = getRepository(dependencyName)
        String commit = repo.tag.list().find { it.name == version }.commit.id
        repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
    }

    List<Version> getVersionList(DeclaredDependency declaredDependency) {
        downloadRepository(declaredDependency)
        def repo = getRepository(declaredDependency.name)
        repo.tag.list().collect { Version.parse(it.name as String) }
    }

    File getSourceFolder(Dependency dependency) {
        checkoutVersion(dependency.name, dependency.version.fullVersion)
        getRepoPath(dependency.name)
    }

    Dependency loadDependency(DeclaredDependency declaredDependency, Dependency parent) {
        downloadRepository(declaredDependency)
        String dependencyName = declaredDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this, parent: parent)

        dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, getVersionList(declaredDependency))
        dependency.downloadUrl = declaredDependency.url ? declaredDependency.url : getDependencyJson(declaredDependency.name).url

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

}
