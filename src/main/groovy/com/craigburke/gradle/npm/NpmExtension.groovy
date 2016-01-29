package com.craigburke.gradle.npm

class NpmExtension {

    String repositoryUrl = 'https://registry.npmjs.org'
    String installDir
    String cacheDir = 'build/npm-cache'

    List<Dependency> rootDependencies = []

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

        Dependency dependency = new Dependency(name: dependencyName, versionExpression: version)

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(dependency, dependency, dependency)
            use (SourceCategory) {
                clonedClosure()
            }
        }

        rootDependencies += dependency
    }

}
