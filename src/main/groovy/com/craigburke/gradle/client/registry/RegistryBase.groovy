package com.craigburke.gradle.client.registry

import jsr166y.ForkJoinPool

class RegistryBase {

    String registryUrl
    String cachePath
    String installPath

    RegistryBase(String url) {
        registryUrl = url
    }

    static ForkJoinPool pool = new ForkJoinPool(10)

    static String formatPath(String path) {
        path.replace('\\', '/').replace('//', '/')
    }

    String getMainFolderPath(String dependencyName) {
        formatPath("${cachePath}/${dependencyName}")
    }

    static String getDestinationPath(String relativePath, String source, String destination) {
        boolean maintainPath = source.contains('**')

        String adjustedPath
        if (source == '**' || !maintainPath) {
            adjustedPath = relativePath
        }
        else {
            adjustedPath = relativePath - source.tokenize('**').first()
        }

        String fileName = adjustedPath.contains('/') ? adjustedPath.tokenize('/').last() : adjustedPath

        boolean destinationIsFolder = destination?.endsWith('/') || !destination
        boolean absolutePath = destination?.startsWith('/')

        String path = absolutePath ? "..${destination}" : destination

        if (destinationIsFolder) {
            path += maintainPath ? adjustedPath : fileName
        }

        path
    }


}