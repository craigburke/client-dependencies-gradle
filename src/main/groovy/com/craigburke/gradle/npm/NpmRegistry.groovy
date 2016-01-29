package com.craigburke.gradle.npm

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.file.FileCopyDetails

import static groovyx.gpars.GParsPool.withExistingPool

class NpmRegistry implements Registry {

    private static String normalizeExpression(String expression) {
        expression?.startsWith('./') ? expression.substring(2) : expression
    }

    private String getDependencySourceFolder(Dependency dependency) {
        "${cacheDir}/${dependency.name}/${dependency.version.fullVersion}"
    }

    private String getDownloadFilePath(Dependency dependency) {
        "${getDependencySourceFolder(dependency)}/package.tgz"
    }

    Dependency loadDependency(Dependency dependency) {
        Dependency loadedDependency = dependency.clone()

        String mainConfigPath = "${cacheDir}/${loadedDependency.name}/package.json"
        File mainConfigFile = project.file(mainConfigPath)

        def versionListJson

        if (mainConfigFile.exists()) {
            versionListJson = new JsonSlurper().parse(mainConfigFile).versions
        }
        else {
            URL url = new URL("${repositoryUrl}/${loadedDependency.name}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            versionListJson = json.versions
        }

        List<Version> versionList = versionListJson.collect { new Version(it.key as String) }
        loadedDependency.version = VersionResolver.findMax(dependency.versionExpression, versionList)
        def versionJson = versionListJson[loadedDependency.version.fullVersion]
        loadedDependency.downloadUrl = versionJson.dist.tarball

        File versionConfigFile = project.file("${getDependencySourceFolder(loadedDependency)}/package/package.json")

        if (!versionConfigFile.exists()) {
            versionConfigFile.parentFile.mkdirs()
            versionConfigFile.text = JsonOutput.toJson(versionJson).toString()
        }

        withExistingPool(pool) {
            loadedDependency.children = versionJson.dependencies.collectParallel { String name, String childVersion ->
                Dependency childDependency = new Dependency(name: name, versionExpression: childVersion, parent: dependency)
                loadDependency(childDependency)
            } ?: []
        }

        loadedDependency
    }

    void downloadDependency(Dependency dependency) {
        File downloadFile = project.file(getDownloadFilePath(dependency))

        if (!downloadFile.exists()) {
            downloadFile.parentFile.mkdirs()
            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }
        }
    }

    void installDependency(Dependency dependency) {
        project.file(installDir).mkdirs()

        dependency.sources.each { String source, String destination ->
            installDependencySource(dependency, source, destination)
        }

        if (!dependency.sources) {
            installDependencySource(dependency, 'dist/*', '')
        }
    }

    private void installDependencySource(Dependency dependency, String source, String destination) {
        String normalizedSource = normalizeExpression(source)
        String includeExpression = "package/${normalizedSource}${normalizedSource.endsWith('/') ? '**' : ''}"

        project.copy {
            from project.tarTree(getDownloadFilePath(dependency))
            include includeExpression
            into "${installDir}/${dependency.name}/"
            eachFile { FileCopyDetails fileCopyDetails ->
                String relativePath = fileCopyDetails.path - 'package/'
                fileCopyDetails.path = getDestinationPath(relativePath, source, destination)
            }
        }
    }

}
