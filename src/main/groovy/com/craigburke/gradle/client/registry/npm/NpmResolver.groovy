package com.craigburke.gradle.client.registry.npm

import static com.craigburke.gradle.client.registry.core.RegistryUtil.getShaHash
import static com.craigburke.gradle.client.registry.npm.NpmUtil.extractTarball

import com.craigburke.gradle.client.registry.core.Resolver
import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.DownloadVerifyException
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
        def versionListJson = dependency.registry.loadInfo(dependency)?.versions
        versionListJson.collect { Version.parse(it.key as String) }
    }

    void loadSource(Dependency dependency) {
        File sourceFolder = dependency.sourceDir
        String versionKey = dependency.version.fullVersion
        Map versionsJson = dependency.info.versions
        Map distJson = versionsJson?.containsKey(versionKey) ? versionsJson[versionKey].dist : [:]
        String downloadUrl = dependency.url ?: distJson?.tarball

        if (downloadUrl.endsWith('tgz')) {
            log.info "Downloading ${dependency} from ${downloadUrl}"

            String fileName = "${sourceFolder.absolutePath}/package.tgz"
            File downloadFile = new File(fileName)

            downloadFile.withOutputStream { out ->
                out << new URL(downloadUrl).openStream()
            }

            if (distJson?.shasum) {
                verifyFileChecksum(downloadFile, distJson.shasum as String)
            }

            extractTarball(downloadFile, sourceFolder)
            downloadFile.delete()
        } else {
            log.info "Downloading ${dependency} from ${downloadUrl}"
            Grgit.clone(dir: sourceFolder.absolutePath, uri: downloadUrl, refToCheckout: 'master')
        }

    }

    private verifyFileChecksum(File downloadFile, String checksum) {
        log.info "Verifying checksum for file ${downloadFile.absolutePath} [${checksum}]"
        if (getShaHash(downloadFile.bytes) != checksum) {
            throw new DownloadVerifyException("${downloadFile.absolutePath} doesn't match checksum ${checksum}")
        }
    }

}
