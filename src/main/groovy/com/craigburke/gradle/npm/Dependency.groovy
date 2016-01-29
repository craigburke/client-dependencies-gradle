package com.craigburke.gradle.npm

import groovy.transform.AutoClone

@AutoClone
class Dependency {
    String name
    String versionExpression
    String downloadUrl
    Version version

    Map sources = [:]
    List excludes = []

    Dependency parent
    List<Dependency> children = []

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
