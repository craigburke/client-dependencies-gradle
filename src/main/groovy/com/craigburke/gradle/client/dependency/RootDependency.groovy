package com.craigburke.gradle.client.dependency

class RootDependency extends SimpleDependency {

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
