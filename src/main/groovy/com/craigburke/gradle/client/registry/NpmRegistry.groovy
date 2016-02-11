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

    private getVersionJson(String dependencyName, Version version) {
        getVersionListJson(dependencyName)[version.simpleVersion]
    }

    Dependency loadDependency(SimpleDependency simpleDependency) {
        String dependencyName = simpleDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)

        if (simpleDependency.url) {
            dependency.version = new Version(simpleDependency.versionExpression)
        }
        else {
            dependency.version = VersionResolver.resolve(simpleDependency.versionExpression, getVersionList(simpleDependency))
        }

        if (!dependency.version) {
            throw new Exception("Couldn't resolve ${dependencyName}@${simpleDependency.versionExpression}")
        }

        def versionJson
        if (simpleDependency.url) {
            dependency.downloadUrl = simpleDependency.url
        }
        else {
            versionJson = getVersionJson(dependencyName, dependency.version)
            dependency.downloadUrl = versionJson.dist.tarball
        }

        if (simpleDependency.transitive) {
            dependency.children = loadChildDependencies(simpleDependency, dependency.version)
        }

        dependency
    }

    private Map<String, String> getDependencies(SimpleDependency simpleDependency, Version version) {
        if (simpleDependency.url) {
            return [:]
        }
        else {
            getVersionJson(simpleDependency.name, version).dependencies as Map<String, String>
        }
    }

    private List<Dependency> loadChildDependencies(SimpleDependency simpleDependency, Version version) {

        withExistingPool(pool) {
            getDependencies(simpleDependency, version)
                    .findAll { String name, String childVersion -> !simpleDependency.excludes.contains(name)}
                    .collectParallel { String name, String childVersion ->
                SimpleDependency childDependency = new SimpleDependency(name: name, versionExpression: childVersion, excludes: simpleDependency.excludes)
                loadDependency(childDependency)
            } ?: []
        } as List<Dependency>
    }

    List<Version> getVersionList(SimpleDependency dependency) {
        if (dependency.url) {
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
