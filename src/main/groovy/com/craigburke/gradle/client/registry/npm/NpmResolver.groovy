package com.craigburke.gradle.client.registry.npm

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import com.craigburke.gradle.client.registry.core.ResolverBase
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.api.logging.Logger

/**
 *
 * General resolver for NPM
 *
 * @author Craig Burke
 */
class NpmResolver extends ResolverBase implements Resolver {

    private final Logger log

    NpmResolver(Logger log) {
        this.log = log
    }

    boolean canResolve(Dependency dependency) {
        (dependency != null)
    }

    private static getVersionListFromNpm(Dependency dependency) {
        URL url = new URL("${dependency.registry.url}/${dependency.name}")
        new JsonSlurper().parse(url).versions
    }

    private String getDownloadUrlFromNpm(Dependency dependency) {
        getVersionListFromNpm(dependency)[dependency.version.fullVersion]?.dist?.tarball
    }

    List<Version> getVersionList(Dependency dependency) {
        def versionListJson = getVersionListFromNpm(dependency)
        versionListJson.collect { Version.parse(it.key as String) }
    }

    void downloadDependency(Dependency dependency) {
        withLock(dependency) {
            File sourceFolder = dependency.sourceFolder

            if (sourceFolder.exists()) {
                return
            }

            sourceFolder.mkdirs()

            String downloadUrl = dependency.url ?: getDownloadUrlFromNpm(dependency)

            if (downloadUrl.endsWith('tgz')) {
                String fileName = "${sourceFolder.absolutePath}/package.tgz"
                File downloadFile = new File(fileName)
                downloadFile.parentFile.mkdirs()

                downloadFile.withOutputStream { out ->
                    out << new URL(downloadUrl).openStream()
                }

                AntBuilder builder = new AntBuilder()
                builder.project.buildListeners.first().setMessageOutputLevel(0)
                builder.untar(src: downloadFile.absolutePath,  dest: sourceFolder.absolutePath,
                        compression: 'gzip', overwrite: true) {
                    patternset {
                        include(name: 'package/**')
                    }
                    mapper {
                        globmapper(from: 'package/*', to: '*')
                    }
                }

                downloadFile.delete()
            }
            else {
                Grgit.clone(dir: sourceFolder.absolutePath, uri: dependency.url, refToCheckout: 'master')
            }
        }

    }
}
