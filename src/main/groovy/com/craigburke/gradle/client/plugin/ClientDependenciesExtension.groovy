package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.RootDependency
import com.craigburke.gradle.client.registry.Registry


class ClientDependenciesExtension {

    Registry registry
    String installDir
    String cacheDir
    List<RootDependency> rootDependencies = []

    class SourceCategory {
        static Map rightShift(String source, String path) {
            ["${source}": path]
        }
    }

    def methodMissing(String name, args) {
        String version
        String dependencyName = name

        if (dependencyName.contains(':')) {
            (dependencyName, version) = dependencyName.tokenize(':')
        }
        else if (args instanceof String) {
            version = args
        }
        else if (args) {
            version = args.first() instanceof String ? args.first() : 'latest'
        }
        else {
            version = 'latest'
        }

        RootDependency dependency = new RootDependency(name: dependencyName, versionExpression: version)

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(dependency, dependency, dependency)
            use (SourceCategory) {
                clonedClosure()
            }
        }

        rootDependencies += dependency
    }

}
