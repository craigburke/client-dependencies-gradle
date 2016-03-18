package com.craigburke.gradle.client.registry.npm

/**
 *
 * Util class for NPM
 *
 * @author Craig Burke
 */
class NpmUtil {

    static void extractTarball(File sourceFile, File destination) {
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
