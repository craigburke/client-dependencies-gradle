package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.RootDependency
import com.craigburke.gradle.client.registry.Registry


class ClientDependenciesExtension {

    String installDir
    String cacheDir

    Map<String, Registry> registryMap = [:]
    List<RootDependency> rootDependencies = []

    class SourceCategory {
        static Map rightShift(String source, String path) {
            ["${source}": path]
        }
    }

    def methodMissing(String registryName, args) {
        Registry registry = registryMap[registryName]

        def (dependencyName, version) = args.first().tokenize(':')

        RootDependency dependency = new RootDependency(
                name: dependencyName,
                versionExpression: version,
                registry: registry
        )

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(dependency, dependency, dependency)
            use (SourceCategory) {
                clonedClosure()
            }
        }

        rootDependencies += dependency
    }

}
