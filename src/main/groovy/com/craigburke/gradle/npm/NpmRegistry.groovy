package com.craigburke.gradle.npm

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

    private String getMainFolderPath(Dependency dependency) {
        "${cacheDir}/${dependency.name}"
    }

    private File getDependencyInfoFile(Dependency dependency) {
        project.file("${getMainFolderPath(dependency)}/${dependency.version.fullVersion}/package/package.json")
    }

    private File getDownloadFile(Dependency dependency) {
        project.file("${getMainFolderPath(dependency)}/${dependency.version.fullVersion}/package.tgz")
    }

    private getVersionListJson(Dependency dependency) {
        String mainConfigPath = "${getMainFolderPath(dependency)}/main.json"
        File mainConfigFile = project.file(mainConfigPath)

        def versionListJson

        if (mainConfigFile.exists()) {
            versionListJson = new JsonSlurper().parse(mainConfigFile).versions
        }
        else {
            URL url = new URL("${repositoryUrl}/${dependency.name}")
            def json = new JsonSlurper().parse(url)

            mainConfigFile.parentFile.mkdirs()
            mainConfigFile.text = JsonOutput.toJson(json).toString()
            versionListJson = json.versions
        }
        versionListJson
    }

    Dependency loadDependency(Dependency dependency) {
        Dependency loadedDependency = dependency.clone()

        def versionListJson = getVersionListJson(dependency)
        List<Version> versionList = versionListJson.collect { new Version(it.key as String) }
        loadedDependency.version = VersionResolver.findMax(dependency.versionExpression, versionList)

        def versionJson = versionListJson[loadedDependency.version.fullVersion]
        loadedDependency.downloadUrl = versionJson.dist.tarball

        File versionConfigFile = getDependencyInfoFile(loadedDependency)
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
        File downloadFile = getDownloadFile(dependency)

        if (!downloadFile.exists()) {
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }
        }
    }

    void installDependency(Dependency dependency) {
        downloadDependency(dependency)

        project.file(installDir).mkdirs()
        dependency.sources.each { String source, String destination ->
            installDependencySource(dependency, source, destination)
        }
        if (!dependency.sources) {
            installDependencySource(dependency, '**', '')
        }
    }

    private void installDependencySource(Dependency dependency, String source, String destination) {
        String normalizedSource = normalizeExpression(source)
        String includeExpression = "${normalizedSource}${normalizedSource.endsWith('/') ? '**' : ''}"

        project.copy {
            from project.tarTree(getDownloadFile(dependency)).files
            include includeExpression
            into "${installDir}/${dependency.name}/"
            eachFile { FileCopyDetails fileCopyDetails ->
                String relativePath = fileCopyDetails.path - '/package'
                fileCopyDetails.path = getDestinationPath(relativePath, source, destination)
            }
        }
    }

}
