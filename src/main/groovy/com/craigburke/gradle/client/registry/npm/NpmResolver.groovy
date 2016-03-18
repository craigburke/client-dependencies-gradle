package com.craigburke.gradle.client.registry.npm

import static com.craigburke.gradle.client.registry.core.ResolverUtil.withLock
import static com.craigburke.gradle.client.registry.npm.NpmUtil.extractTarball

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.api.logging.Logger

/**
 *
 * General resolver for NPM
 *
 * @author Craig Burke
 */
class NpmResolver implements Resolver {

    private final Logger log

    NpmResolver(Logger log) {
        this.log = log
    }

    boolean canResolve(Dependency dependency) {
        (dependency != null)
    }

    List<Version> getVersionList(Dependency dependency) {
        def versionListJson = getVersionListFromNpm(dependency)
        versionListJson.collect { Version.parse(it.key as String) }
    }

    void downloadDependency(Dependency dependency) {
        withLock(dependency.key) {
            File sourceFolder = dependency.sourceFolder

            if (sourceFolder.exists()) {
                return
            }
            sourceFolder.mkdirs()
            String downloadUrl = dependency.url ?: getDownloadUrlFromNpm(dependency)

            if (downloadUrl.endsWith('tgz')) {
                log.info "Downloading ${dependency} from ${downloadUrl}"

                String fileName = "${sourceFolder.absolutePath}/package.tgz"
                File downloadFile = new File(fileName)

                downloadFile.withOutputStream { out ->
                    out << new URL(downloadUrl).openStream()
                }

                extractTarball(downloadFile, sourceFolder)
                downloadFile.delete()
            } else {
                log.info "Downloading ${dependency} from ${downloadUrl}"
                Grgit.clone(dir: sourceFolder.absolutePath, uri: dependency.url, refToCheckout: 'master')
            }
        }
    }

    private static getVersionListFromNpm(Dependency dependency) {
        URL url = new URL("${dependency.registry.url}/${dependency.name}")
        new JsonSlurper().parse(url).versions
    }

    private static String getDownloadUrlFromNpm(Dependency dependency) {
        getVersionListFromNpm(dependency)[dependency.version.fullVersion]?.dist?.tarball
    }

}
