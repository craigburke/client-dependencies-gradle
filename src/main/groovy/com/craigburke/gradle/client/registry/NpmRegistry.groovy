package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import static groovyx.gpars.GParsPool.withExistingPool

class NpmRegistry extends RegistryBase implements Registry {

    NpmRegistry(String url = 'https://registry.npmjs.org') {
        super(url)
        sourcePathPrefix = 'package/'
    }

    private File getDependencyInfoFile(Dependency dependency) {
        new File("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package/package.json")
    }

    private File getDownloadFile(Dependency dependency) {
        new File("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package.tgz")
    }

    private getVersionListJson(String dependencyName) {
        File mainConfigFile = new File("${getMainFolderPath(dependencyName)}/main.json")

        def versionListJson

        if (mainConfigFile.exists()) {
            versionListJson = new JsonSlurper().parse(mainConfigFile).versions
        } else {
            URL url = new URL("${this.registryUrl}/${dependencyName}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            versionListJson = json.versions
        }
        versionListJson
    }

    private getVersionJson(String dependencyName, String version) {
        getVersionListJson(dependencyName)[version]
    }

    Dependency loadDependency(SimpleDependency simpleDependency) {
        String dependencyName = simpleDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)
        dependency.version = VersionResolver.resolve(simpleDependency.versionExpression, getVersionList(simpleDependency))
        if (!dependency.version) {
            throw new Exception("Couldn't resolve ${dependencyName}@${simpleDependency.versionExpression}")
        }
        def versionJson = getVersionJson(dependencyName, dependency.version.fullVersion)
        dependency.downloadUrl = versionJson.dist.tarball

        File versionConfigFile = getDependencyInfoFile(dependency)
        if (!versionConfigFile.exists()) {
            versionConfigFile.parentFile.mkdirs()
            versionConfigFile.text = JsonOutput.toJson(versionJson).toString()
        }

        if (simpleDependency.transitive && !simpleDependency.gitDependency) {
            dependency.children = loadChildDependencies(versionJson, simpleDependency.excludes)
        }

        dependency
    }

    private List<Dependency> loadChildDependencies(versionJson, List<String> exclusions) {
        withExistingPool(pool) {
            versionJson.dependencies
                    .findAll { String name, String childVersion -> !exclusions.contains(name)}
                    .collectParallel { String name, String childVersion ->
                SimpleDependency childDependency = new SimpleDependency(name: name, versionExpression: childVersion, excludes: exclusions)
                loadDependency(childDependency)
            } ?: []
        } as List<Dependency>
    }

    List<Version> getVersionList(SimpleDependency dependency) {
        if (dependency.gitDependency) {
            []
        }
        else {
            def versionListJson = getVersionListJson(dependency.name)
            versionListJson.collect { new Version(it.key as String) }
        }
    }

    void downloadDependency(Dependency dependency) {
        File downloadFile = getDownloadFile(dependency)

        if (!downloadFile.exists()) {
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }
        }
    }

    File getInstallSource(Dependency dependency) {
        downloadDependency(dependency)
        getDownloadFile(dependency)
    }

}
