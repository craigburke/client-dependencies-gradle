package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.RootDependency
import com.craigburke.gradle.client.registry.Registry

class DependencyBuilder {

    Registry registry
    List<RootDependency> rootDependencies = []

    DependencyBuilder(Registry registry) {
        this.registry = registry
    }

    class SourceCategory {
        static Map rightShift(String source, String path) {
            ["${source}": path]
        }
    }

    def methodMissing(String name, args) {
        String version
        String dependencyName

        if (name.contains(':')) {
            (dependencyName, version) = name.tokenize(':')
        }
        else if (args && args.first() instanceof String) {
            dependencyName = name
            version = args.first()
        }

        RootDependency dependency = new RootDependency(name: dependencyName, versionExpression: version, registry: registry)

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(dependency, dependency, dependency)
            use (SourceCategory) {
                clonedClosure()
            }
        }

        rootDependencies += dependency
    }

}
