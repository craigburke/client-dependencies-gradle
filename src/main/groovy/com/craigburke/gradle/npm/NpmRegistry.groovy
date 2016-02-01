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

    private String getDependencyFolder(Dependency dependency) {
        "${cacheDir}/${dependency.name}/${dependency.version.fullVersion}"
    }


    private String getSourceFolder(Dependency dependency) {
        "${getDependencyFolder(dependency)}/source/"
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

        File versionConfigFile = project.file("${getDependencyFolder(loadedDependency)}/package/package.json")

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
        File dependencySourceFolder = project.file(getSourceFolder(dependency))
        File downloadFile = project.file("${getDependencyFolder(dependency)}/package/package.tgz")

        if (!downloadFile.exists()) {
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }
        }

        if (!dependencySourceFolder.exists()) {
            dependencySourceFolder.mkdirs()

            project.copy {
                from project.tarTree(downloadFile.absolutePath).files
                into dependencySourceFolder.absolutePath
                eachFile { FileCopyDetails fileCopyDetails ->
                    fileCopyDetails.path -= 'package/'
                }
            }
        }
    }

    void installDependency(Dependency dependency) {
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
            from project.file(getSourceFolder(dependency))
            include includeExpression
            into "${installDir}/${dependency.name}/"
            eachFile { FileCopyDetails fileCopyDetails ->
                fileCopyDetails.path = getDestinationPath(fileCopyDetails.path, source, destination)
            }
        }
    }

}
