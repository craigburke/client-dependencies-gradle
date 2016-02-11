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
        Map props = [:]
        props.registry = registry

        if (name.contains(':')) {
            List<String> nameParts = name.tokenize(":")
            props.name = nameParts[0]
            props.versionExpression = nameParts[1]
        }
        else {
            props.name = name
            props.versionExpression = args.find { it instanceof String }
        }

        Map additionalProps = args.find { it instanceof Map }
        if (additionalProps) {
            props += additionalProps
        }

        RootDependency dependency = new RootDependency(props)

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(dependency, dependency, dependency)
            use (SourceCategory) {
                clonedClosure()
            }
        }

        rootDependencies += dependency
    }

}
