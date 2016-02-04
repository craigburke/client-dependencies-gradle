package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.ResetOp

class BowerRegistry implements Registry {

    BowerRegistry(String url = 'https://bower.herokuapp.com') {
        repositoryUrl = url
    }

    private getDependencyJson(String dependencyName) {
        File mainConfigFile = new File("${getMainFolderPath(dependencyName)}/main.json")

        def dependencyJson

        if (mainConfigFile.exists()) {
            dependencyJson = new JsonSlurper().parse(mainConfigFile)
        } else {
            URL url = new URL("${repositoryUrl}/packages/${dependencyName}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            dependencyJson = json
        }
        dependencyJson
    }

    private File getRepoPath(String dependencyName) {
       new File("${getMainFolderPath(dependencyName)}/source/")
    }

    private Grgit getRepository(String dependencyName) {
        def dependencyJson = getDependencyJson(dependencyName)
        String gitUrl = dependencyJson.url

        File repoPath = getRepoPath(dependencyName)
        Grgit repo

        if (!repoPath.exists()) {
            repoPath.mkdirs()
            repo = Grgit.clone(dir: repoPath.absolutePath, uri: gitUrl, refToCheckout: 'master')
        }
        else {
            repo = Grgit.open(dir: repoPath.absolutePath)
        }
        repo
    }

    void checkoutVersion(String dependencyName, String version) {
        def repo = getRepository(dependencyName)
        String commit = repo.tag.list().find { it.name == version }.commit.id
        repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
    }

    List<Version> getVersionList(String dependencyName) {
        def repo = getRepository(dependencyName)
        repo.tag.list().collect { new Version(it.name as String) }
    }

    File getCopySource(Dependency dependency) {
        checkoutVersion(dependency.name, dependency.version.fullVersion)
        getRepoPath(dependency.name)
    }

    Dependency loadDependency(SimpleDependency simpleDependency) {
        String dependencyName = simpleDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)

        def dependencyJson = getDependencyJson(simpleDependency.name)

        dependency.version = VersionResolver.resolve(simpleDependency.versionExpression, getVersionList(dependencyName))
        dependency.downloadUrl = dependencyJson.url

        dependency
    }

}
