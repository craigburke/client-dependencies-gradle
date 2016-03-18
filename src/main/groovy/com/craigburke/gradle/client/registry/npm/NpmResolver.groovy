package com.craigburke.gradle.client.registry.npm

import static com.craigburke.gradle.client.registry.core.ResolverUtil.getShaHash
import static com.craigburke.gradle.client.registry.core.ResolverUtil.withLock
import static com.craigburke.gradle.client.registry.npm.NpmUtil.extractTarball

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import com.craigburke.gradle.client.registry.core.DownloadVerifyException
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
            DownloadInfo download = getDownloadInfo(dependency)

            if (download.url.endsWith('tgz')) {
                log.info "Downloading ${dependency} from ${download.url}"

                String fileName = "${sourceFolder.absolutePath}/package.tgz"
                File downloadFile = new File(fileName)

                downloadFile.withOutputStream { out ->
                    out << new URL(download.url).openStream()
                }

                if (download.checksum) {
                    verifyFileChecksum(downloadFile, download.checksum)
                }

                extractTarball(downloadFile, sourceFolder)
                downloadFile.delete()
            } else {
                log.info "Downloading ${dependency} from ${download.url}"
                Grgit.clone(dir: sourceFolder.absolutePath, uri: download.url, refToCheckout: 'master')
            }
        }
    }

    private verifyFileChecksum(File downloadFile, String checksum) {
        log.info "Verifying checksum for file ${downloadFile.absolutePath} [${checksum}]"
        if (getShaHash(downloadFile.bytes) != checksum) {
            throw new DownloadVerifyException("${downloadFile.absolutePath} doesn't match checksum ${checksum}")
        }
    }

    private static getVersionListFromNpm(Dependency dependency) {
        URL url = new URL("${dependency.registry.url}/${dependency.name}")
        new JsonSlurper().parse(url).versions
    }

    private static DownloadInfo getDownloadInfo(Dependency dependency) {
        if (dependency.url) {
            new DownloadInfo(url: dependency.url, checksum: null)
        }
        else {
            def json = getVersionListFromNpm(dependency)[dependency.version.fullVersion]?.dist
            new DownloadInfo(url: json?.tarball, checksum: json?.shasum)
        }
    }

}
