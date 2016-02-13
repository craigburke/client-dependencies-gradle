package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.dependency.VersionResolver
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit

import static groovyx.gpars.GParsPool.withExistingPool

class NpmRegistry extends RegistryBase implements Registry {

    NpmRegistry(String url = 'https://registry.npmjs.org') {
        super(url)
    }

    private String getSourceFolderPath(Dependency dependency) {
       "${getMainFolderPath(dependency.name)}/${dependency.version}/source/"
    }

    private getVersionListFromNpm(String dependencyName) {
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

    private getVersionInfoFromNpm(String dependencyName, Version version) {
        getVersionListFromNpm(dependencyName)[version.fullVersion]
    }

    Dependency loadDependency(SimpleDependency simpleDependency) {
        String dependencyName = simpleDependency.name
        Dependency dependency = new Dependency(name: dependencyName, registry: this)

        if (simpleDependency.url) {
            dependency.version = Version.parse(simpleDependency.versionExpression)
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
            versionJson = getVersionInfoFromNpm(dependencyName, dependency.version)
            dependency.downloadUrl = versionJson.dist.tarball
        }

        downloadDependency(dependency)

        if (simpleDependency.transitive) {
            dependency.children = loadChildDependencies(dependency, simpleDependency.excludes)
        }

        dependency
    }

    private Map<String, String> getDependencies(Dependency dependency) {
        File packageJson = new File("${getSourceFolderPath(dependency)}/package.json")

        if (packageJson.exists()) {
            def json = new JsonSlurper().parse(packageJson)
            json.dependencies
        }
        else {
            [:]
        }
    }

    private List<Dependency> loadChildDependencies(Dependency dependency, List<String> excludes) {
        withExistingPool(pool) {
            getDependencies(dependency)
                    .findAll { String name, String childVersion -> !excludes.contains(name)}
                    .collectParallel { String name, String childVersion ->
                SimpleDependency childDependency = new SimpleDependency(name: name, versionExpression: childVersion, excludes: excludes)
                loadDependency(childDependency)
            } ?: []
        } as List<Dependency>
    }

    List<Version> getVersionList(SimpleDependency dependency) {
        if (dependency.url) {
            [Version.parse(dependency.versionExpression)]
        }
        else {
            def versionListJson = getVersionListFromNpm(dependency.name)
            versionListJson.collect { Version.parse(it.key as String) }
        }
    }

    void downloadDependency(Dependency dependency) {
        File sourceFolder = new File("${getSourceFolderPath(dependency)}")

        if (sourceFolder.exists()) {
            return
        }

        sourceFolder.mkdirs()

        if (dependency.downloadUrl.endsWith('tgz')) {
            File downloadFile = new File("${getMainFolderPath(dependency.name)}/${dependency.version.fullVersion}/package.tgz")
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << new URL(dependency.downloadUrl).openStream()
            }

            AntBuilder builder = new AntBuilder()
            builder.project.buildListeners.first().setMessageOutputLevel(0)
            builder.untar(src: downloadFile.absolutePath, dest: sourceFolder.absolutePath, compression:'gzip', overwrite:true) {
                patternset {
                    include(name: 'package/**')
                }
                mapper {
                    globmapper(from: 'package/*', to:'*')
                }
            }

            downloadFile.delete()
        }
        else {
            Grgit.clone(dir: sourceFolder.absolutePath, uri: dependency.downloadUrl, refToCheckout: 'master')
        }
    }

    File getSourceFolder(Dependency dependency) {
        new File("${getSourceFolderPath(dependency)}")
    }

}
