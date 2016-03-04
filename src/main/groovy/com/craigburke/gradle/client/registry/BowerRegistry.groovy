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

import static groovyx.gpars.GParsPool.withExistingPool

import org.gradle.api.logging.Logger
import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.ResetOp
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * Registry to resolves Bower Dependencies
 *
 * @author Craig Burke
 */
class BowerRegistry extends RegistryBase implements Registry {

    static final String DEFAULT_URL = 'https://bower.herokuapp.com'
    private final Map<String, Object> dependencyLocks = [:] as ConcurrentHashMap

    BowerRegistry(String url, Logger log) {
        super(url, log)
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
        File bowerConfigFile = new File("${getRepoPath(dependency)}/bower.json")
        def bowerConfigJson = new JsonSlurper().parse(bowerConfigFile)
        withExistingPool(RegistryBase.pool) {
            bowerConfigJson.dependencies
                    .findAll { String name, String versionExpression -> !exclusions.contains(name) }
                    .collectParallel { String name, String versionExpression ->
                if (dependency.ancestorsAndSelf*.name.contains(name)) {
                    String message = "Circular dependency created by dependency ${name}@${versionExpression}"
                    throw new CircularDependencyException(message)
                }

                DeclaredDependency childDependency = new DeclaredDependency(name: name, versionExpression: versionExpression)
                loadDependency(childDependency, dependency)
            } ?: []
        } as List<Dependency>
    }

    private File getRepoPath(Dependency dependency) {
        new File("${getMainFolderPath(dependency.name)}/source/${dependency.version.fullVersion}/")
    }

    private File getBareRepoFolder(String dependencyName) {
        new File("${getMainFolderPath(dependencyName)}/${dependencyName}.git/")
    }

    private void downloadRepository(DeclaredDependency dependency) {
        log.info "Downloading dependency: ${dependency.name}"

        dependencyLocks.putIfAbsent(dependency.name, new Object())
        Object lock = dependencyLocks[dependency.name]

        synchronized(lock) {
            File bareRepoPath = getBareRepoFolder(dependency.name)

            if (!bareRepoPath.exists()) {

                String gitUrl
                if (dependency.url) {
                    gitUrl = dependency.url
                } else {
                    def dependencyJson = getDependencyJson(dependency.name)
                    gitUrl = dependencyJson.url
                }

                bareRepoPath.mkdirs()
                Grgit.clone(dir: bareRepoPath.absolutePath, uri: gitUrl, bare: true)
            }
        }
    }

    private Grgit getBareRepository(String dependencyName) {
        File repoPath = getBareRepoFolder(dependencyName)
        Grgit.open(dir: repoPath.absolutePath)
    }

    void checkoutVersion(Dependency dependency) {
        log.info "Checking out dependency: ${dependency}"

        dependencyLocks.putIfAbsent(dependency.toString(), new Object())
        Object lock = dependencyLocks[dependency.toString()]

        synchronized(lock) {
            File repoPath = getRepoPath(dependency)

            if (!repoPath.exists()) {
                String gitUrl = "file://${getBareRepoFolder(dependency.name).absolutePath}"
                Grgit repo = Grgit.clone(dir: repoPath.absolutePath, uri: gitUrl)
                String commit = repo.tag.list().find { (it.name - 'v') == dependency.version.fullVersion }.commit.id
                repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
            }
        }

    }

    List<Version> getVersionList(DeclaredDependency declaredDependency) {
        downloadRepository(declaredDependency)
        Grgit repo = getBareRepository(declaredDependency.name)
        repo.tag.list().collect { Version.parse(it.name as String) }
    }

    File getSourceFolder(Dependency dependency) {
        checkoutVersion(dependency)
        getRepoPath(dependency)
    }

    Dependency loadDependency(DeclaredDependency declaredDependency, Dependency parent) {
        log.info "Loading dependency: ${declaredDependency}"

        String name = declaredDependency.name
        downloadRepository(declaredDependency)

        Dependency dependency = new Dependency(name: name, registry: this, parent: parent,
                versionExpression: declaredDependency.versionExpression)

        dependency.version = VersionResolver.resolve(declaredDependency.versionExpression, getVersionList(declaredDependency))

        checkoutVersion(dependency)
        dependency.downloadUrl = declaredDependency.url ? declaredDependency.url : getDependencyJson(declaredDependency.name).url

        if (declaredDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, declaredDependency.exclude)
        }

        dependency
    }

}
