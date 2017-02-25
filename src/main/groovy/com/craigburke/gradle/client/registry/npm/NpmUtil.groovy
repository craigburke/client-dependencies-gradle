package com.craigburke.gradle.client.registry.npm

import groovy.io.FileType

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
                compression: 'gzip', overwrite: true)

        // move files and folders from a subdirectory (such as package) into the root of the destination folder
        destination.eachFile(FileType.DIRECTORIES) { File rootDirectory ->
            rootDirectory.eachFile { File file ->
                file.renameTo(destination.absolutePath + File.separator + file.name)
            }
        }

    }

}
