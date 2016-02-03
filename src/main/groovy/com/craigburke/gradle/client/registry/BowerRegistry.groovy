package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version

class BowerRegistry implements Registry {

    List<Version> getVersionList(String dependencyName) {
        return null
    }

    void installDependency(Dependency dependency, Map sources) {

    }

    Dependency loadDependency(SimpleDependency dependency) {
        return null
    }

    void installDependency(Dependency dependency) {

    }

}
