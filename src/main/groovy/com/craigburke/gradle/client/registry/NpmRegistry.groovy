package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.file.FileCopyDetails

import static groovyx.gpars.GParsPool.withExistingPool

class NpmRegistry implements Registry {

    NpmRegistry(String url = 'https://registry.npmjs.org') {
        repositoryUrl = url
    }

    private static String normalizeExpression(String expression) {
        expression?.startsWith('./') ? expression.substring(2) : expression
    }

    private String getMainFolderPath(String dependencyName) {
        "${cacheDir}/${dependencyName}"
    }

    private File getDependencyInfoFile(Dependency dependency) {
        project.file("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package/package.json")
    }

    private File getDownloadFile(Dependency dependency) {
        project.file("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package.tgz")
    }

    private getVersionListJson(String dependencyName) {
        String mainConfigPath = "${getMainFolderPath(dependencyName)}/main.json"
        File mainConfigFile = project.file(mainConfigPath)

        def versionListJson

        if (mainConfigFile.exists()) {
            versionListJson = new JsonSlurper().parse(mainConfigFile).versions
        } else {
            URL url = new URL("${repositoryUrl}/${dependencyName}")
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
        Dependency dependency = new Dependency(name: dependencyName)
        dependency.version = VersionResolver.resolve(simpleDependency.versionExpression, getVersionList(dependencyName))
        def versionJson = getVersionJson(dependencyName, dependency.version.fullVersion)
        dependency.downloadUrl = versionJson.dist.tarball

        File versionConfigFile = getDependencyInfoFile(dependency)
        if (!versionConfigFile.exists()) {
            versionConfigFile.parentFile.mkdirs()
            versionConfigFile.text = JsonOutput.toJson(versionJson).toString()
        }

        withExistingPool(pool) {
            dependency.children = versionJson.dependencies.collectParallel { String name, String childVersion ->
                Dependency childDependency = new Dependency(name: name, versionExpression: childVersion, parent: dependency)
                loadDependency(childDependency)
            } ?: []
        }

        dependency
    }

    List<Version> getVersionList(String dependencyName) {
        def versionListJson = getVersionListJson(dependencyName)
        versionListJson.collect { new Version(it.key as String) }
    }

    Dependency loadDependency(Dependency dependency) {
        Dependency loadedDependency = dependency.clone()

        loadedDependency.version = VersionResolver.findMax(dependency.versionExpression, getVersionList(dependency.name))
        def versionJson = getVersionJson(dependency.name, loadedDependency.version.fullVersion)

        loadedDependency.downloadUrl = versionJson.dist.tarball

        File versionConfigFile = getDependencyInfoFile(loadedDependency)
        if (!versionConfigFile.exists()) {
            versionConfigFile.parentFile.mkdirs()
            versionConfigFile.text = JsonOutput.toJson(versionJson).toString()
        }

        withExistingPool(pool) {
            loadedDependency.children = versionJson.dependencies.collectParallel { String name, String childVersion ->
                Dependency childDependency = new SimpleDependency(name: name, versionExpression: childVersion)
                loadDependency(childDependency)
            } ?: []
        }

        loadedDependency
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

    void installDependency(Dependency dependency, Map sources) {
        downloadDependency(dependency)
        project.file(installDir).mkdirs()

        sources.each { String source, String destination ->
            installDependencySource(dependency, source, destination)
        }
    }

    private void installDependencySource(Dependency dependency, String source, String destination) {
        String normalizedSource = normalizeExpression(source)
        String includeExpression = "package/${normalizedSource}"

        project.copy {
            from project.tarTree(getDownloadFile(dependency))
            include includeExpression
            into "${installDir}/${dependency.name}/"
            eachFile { FileCopyDetails fileCopyDetails ->
                String relativePath = fileCopyDetails.path - 'package/'
                fileCopyDetails.path = getDestinationPath(relativePath, source, destination)
            }
        }
    }

}
