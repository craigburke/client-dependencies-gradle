package com.craigburke.gradle.client.registry

import jsr166y.ForkJoinPool

class RegistryBase {

    String registryUrl
    String cachePath
    String installPath

    static ForkJoinPool pool = new ForkJoinPool(10)

    RegistryBase(String url) {
        registryUrl = url
    }

    static String formatPath(String path) {
        path.replace('\\', '/').replace('//', '/')
    }

    String getMainFolderPath(String dependencyName) {
        formatPath("${cachePath}/${dependencyName}")
    }

}