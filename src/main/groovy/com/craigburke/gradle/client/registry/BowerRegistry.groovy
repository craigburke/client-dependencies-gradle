package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
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
            URL url = new URL("${this.registryUrl}/packages/${dependencyName}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            dependencyJson = json
        }
        dependencyJson
    }

    private List<Dependency> loadChildDependencies(String dependencyName, String version, List<String> exclusions) {
        checkoutVersion(dependencyName, version)
        File bowerConfigFile = new File("${getRepoPath(dependencyName)}/bower.json")
        def bowerConfigJson = new JsonSlurper().parse(bowerConfigFile)
        withExistingPool(pool) {
            bowerConfigJson.dependencies
                    .findAll { String name, String versionExpression -> !exclusions.contains(name) }
                    .collectParallel { String name, String versionExpression ->
                        SimpleDependency childDependency = new SimpleDependency(name: name, versionExpression: versionExpression)
                        loadDependency(childDependency)
                    } ?: []
        } as List<Dependency>
    }

    private File getRepoPath(String dependencyName) {
       new File("${getMainFolderPath(dependencyName)}/source/")
    }

    private void downloadRepository(SimpleDependency dependency) {
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

    List<Version> getVersionList(SimpleDependency dependency) {
        downloadRepository(dependency)
        def repo = getRepository(dependency.name)
        repo.tag.list().collect { Version.parse(it.name as String) }
    }

    File getSourceFolder(Dependency dependency) {
        checkoutVersion(dependency.name, dependency.version.fullVersion)
        getRepoPath(dependency.name)
    }

    Dependency loadDependency(SimpleDependency simpleDependency) {
        downloadRepository(simpleDependency)
        String dependencyName = simpleDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)

        dependency.version = VersionResolver.resolve(simpleDependency.versionExpression, getVersionList(simpleDependency))
        dependency.downloadUrl = simpleDependency.url ? simpleDependency.url : getDependencyJson(simpleDependency.name).url

        if (simpleDependency.transitive) {
            dependency.children = loadChildDependencies(dependency.name, dependency.version.fullVersion, simpleDependency.excludes)
        }

        dependency
    }

}
