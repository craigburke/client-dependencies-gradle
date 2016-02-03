package com.craigburke.gradle.client.dependency

import com.craigburke.gradle.client.registry.Registry

class RootDependency extends SimpleDependency {

    Registry registry
    Map sources = [:]
    List excludes = []

    void source(String sourceValue) {
        source([(sourceValue):''])
    }

    void source(Map sourceValue) {
        sources += sourceValue
    }

    void excludes(String exclude) {
        excludes << exclude
    }

    void excludes(List exclude) {
        excludes += exclude
    }
}
