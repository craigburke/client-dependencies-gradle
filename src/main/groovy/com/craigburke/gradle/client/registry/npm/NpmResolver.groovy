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

    private static String getDownloadUrlFromNpm(Dependency dependency) {
        getVersionListFromNpm(dependency)[dependency.version.fullVersion]?.dist?.tarball
    }

    List<Version> getVersionList(Dependency dependency) {
        def versionListJson = getVersionListFromNpm(dependency)
        versionListJson.collect { Version.parse(it.key as String) }
    }

    void downloadDependency(Dependency dependency) {
        withLock(dependency.name) {
            File sourceFolder = dependency.sourceFolder
            if (sourceFolder.exists()) {
                return
            }
            sourceFolder.mkdirs()

            String downloadUrl = dependency.url ?: getDownloadUrlFromNpm(dependency)

            String npmCachePath = "${System.getProperty('user.home')}/.npm"
            String cacheFilePath = "${npmCachePath}/${dependency.name}/${dependency.version.fullVersion}/package.tgz"
            File cacheFile = new File(cacheFilePath)

            if (cacheFile.exists()) {
                log.info "Loading ${dependency} from ${cacheFilePath}"
                extractTarball(cacheFile, sourceFolder)
            }
            else if (downloadUrl.endsWith('tgz')) {
                log.info "Downloading ${dependency} from ${downloadUrl}"

                String fileName = "${sourceFolder.absolutePath}/package.tgz"
                File downloadFile = new File(fileName)

                downloadFile.withOutputStream { out ->
                    out << new URL(downloadUrl).openStream()
                }

                extractTarball(downloadFile, sourceFolder)
                downloadFile.delete()
            }
            else {
                log.info "Downloading ${dependency} from ${downloadUrl}"
                Grgit.clone(dir: sourceFolder.absolutePath, uri: dependency.url, refToCheckout: 'master')
            }
        }
    }

    private void extractTarball(File sourceFile, File destination) {
        AntBuilder builder = new AntBuilder()
        builder.project.buildListeners.first().setMessageOutputLevel(0)
        builder.untar(src: sourceFile.absolutePath,  dest: destination.absolutePath,
                compression: 'gzip', overwrite: true) {
            patternset {
                include(name: 'package/**')
            }
            mapper {
                globmapper(from: 'package/*', to: '*')
            }
        }
    }

}
