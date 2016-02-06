package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import jsr166y.ForkJoinPool

trait Registry {

    String registryUrl
    String cachePath
    String installPath
    String sourcePathPrefix = ''

    static ForkJoinPool pool = new ForkJoinPool(10)

    static String formatPath(String path) {
        path.replace('\\', '/').replace('//', '/')
    }

    String getMainFolderPath(String dependencyName) {
        formatPath("${cachePath}/${dependencyName}")
    }

    String getDestinationPath(String relativePath, String source, String destination) {
        boolean maintainPath = source != '**' && source.contains('**')
        String adjustedPath = relativePath - (maintainPath ? source.tokenize('**').first() : '')
        String fileName = adjustedPath.contains('/') ? adjustedPath.tokenize('/').last() : adjustedPath

        boolean destinationIsFolder = destination?.endsWith('/') || !destination
        boolean absolutePath = destination?.startsWith('/')

        String path = absolutePath ? "..${destination}" : destination

        if (destinationIsFolder) {
            path += maintainPath ? adjustedPath : fileName
        }

        path
    }

    String getSourceIncludeExpression(String sourceExpression) {
        "${sourcePathPrefix}${sourceExpression}"
    }

    abstract File getInstallSource(Dependency dependency)
    abstract List<Version> getVersionList(String dependencyName)
    abstract Dependency loadDependency(SimpleDependency simpleDependency)
}