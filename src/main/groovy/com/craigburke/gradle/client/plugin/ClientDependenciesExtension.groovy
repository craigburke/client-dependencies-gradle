package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.registry.Registry
import org.gradle.api.Project

class ClientDependenciesExtension {

    Project project
    String installDir
    String cacheDir

    ClientDependenciesExtension(Project project) {
        this.project = project
    }

    Map<String, Registry> registryMap = [:]
    List<DeclaredDependency> rootDependencies = []

    def methodMissing(String registryName, args) {
       if (args && args.last() instanceof Closure) {
           Registry registry = registryMap[registryName]
           DependencyBuilder dependencyBuilder = new DependencyBuilder(registry)
           Closure clonedClosure = args.last().rehydrate(dependencyBuilder, this, this)
           clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
           clonedClosure()
           rootDependencies += dependencyBuilder.rootDependencies
       }
    }

}
