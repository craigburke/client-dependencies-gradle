package com.craigburke.gradle.npm

import jsr166y.ForkJoinPool
import org.gradle.api.Project

import static groovyx.gpars.GParsPool.withExistingPool

trait Registry {

    Project project
    String repositoryUrl
    String cacheDir
    String installDir
    ForkJoinPool pool = new ForkJoinPool(10)

    void installDependencies(List<Dependency> dependencies) {
        List<Dependency> rootDependencies = dependencies.asImmutable()

        withExistingPool(pool) {
            List<Dependency> loadedDependencies = rootDependencies
                    .collectParallel { Dependency dependency -> loadDependency(dependency) }

            flattenDependencies(loadedDependencies).eachParallel { Dependency dependency ->
                downloadDependency(dependency)
                installDependency(dependency)
            }

        }
    }

    List<Dependency> flattenDependencies(List<Dependency> dependencies) {
        dependencies + dependencies.findAll { it.children }
                .collect { flattenDependencies(it.children) }
                .unique(false) { it.name }
    }

    static String getDestinationPath(String relativePath, String source, String destination) {

        boolean sourceIsFolder = !source.contains('*')
        boolean maintainPath = source.contains('**')

        boolean destinationIsFolder = destination?.endsWith('/')
        boolean absolutePath = destination?.startsWith('/')

        String fileName = relativePath.contains('/') ? relativePath.tokenize('/').last() : relativePath
        String path = absolutePath ? "..${destination}" : destination

        if (!destination) {
            path = maintainPath ? relativePath : fileName
        }
        else if (destinationIsFolder) {
            if (sourceIsFolder) {
                String pathFromModuleRoot = relativePath - "${source}/"
                path += pathFromModuleRoot
            }
            else {
                path += maintainPath ? relativePath : fileName
            }
        }

        path
    }

    abstract Dependency loadDependency(Dependency dependency)
    abstract void downloadDependency(Dependency dependency)
    abstract void installDependency(Dependency dependency)
}