package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.RootDependency
import com.craigburke.gradle.client.registry.Registry


class ClientDependenciesExtension {

    String installDir
    String cacheDir

    Map<String, Registry> registryMap = [:]
    List<RootDependency> rootDependencies = []

    def methodMissing(String registryName, args) {
       if (args && args.last() instanceof Closure) {
           Registry registry = registryMap[registryName]
           DependencyBuilder dependencyBuilder = new DependencyBuilder(registry)
           Closure clonedClosure = args.last().rehydrate(dependencyBuilder, dependencyBuilder, dependencyBuilder)
           clonedClosure()
           rootDependencies += dependencyBuilder.rootDependencies
       }
    }

}
