package com.craigburke.gradle.npm

import groovy.json.JsonSlurper

class BowerRegistry implements Registry {

    List<Version> getVersionList(Dependency dependency) {
        return null
    }

    Dependency loadDependency(Dependency dependency) {
        Dependency loadedDependency = dependency.clone()

        URL url = new URL("${repositoryUrl}/packages/${loadedDependency.name}")
        def json = new JsonSlurper().parse(url)

        return null
    }

    void installDependency(Dependency dependency) {

    }

}
