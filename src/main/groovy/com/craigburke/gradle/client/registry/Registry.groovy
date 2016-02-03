package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import jsr166y.ForkJoinPool
import org.gradle.api.Project


trait Registry {

    Project project
    String repositoryUrl
    String cacheDir
    String installDir
    ForkJoinPool pool = new ForkJoinPool(10)

    static String getDestinationPath(String relativePath, String source, String destination) {
        boolean maintainPath = source.contains('**')

        boolean destinationIsFolder = destination?.endsWith('/')
        boolean absolutePath = destination?.startsWith('/')

        String fileName = relativePath.contains('/') ? relativePath.tokenize('/').last() : relativePath
        String path = absolutePath ? "..${destination}" : destination

        if (!destination) {
            path = maintainPath ? relativePath : fileName
        }
        else if (destinationIsFolder) {
            List<String> pathParts = source.tokenize("**")
            String pathCorrection = maintainPath && pathParts ? pathParts.first() : (relativePath - fileName)
            path += relativePath - pathCorrection
        }

        path
    }

    String getMainFolderPath(String dependencyName) {
        "${cacheDir}/${dependencyName}"
    }

    static String normalizeExpression(String expression) {
        expression?.startsWith('./') ? expression.substring(2) : expression
    }

    abstract Dependency loadDependency(SimpleDependency simpleDependency)
    abstract List<Version> getVersionList(String dependencyName)
    abstract void installDependency(Dependency dependency, Map sources)
}