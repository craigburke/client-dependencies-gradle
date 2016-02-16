package com.craigburke.gradle.client.dependency

import com.craigburke.gradle.client.registry.Registry

class DeclaredDependency {

    Registry registry
    String name
    String versionExpression
    String url

    List<String> exclude = []
    boolean transitive = true
    Closure copyConfig

    void setExclude(String exclude) {
        this.exclude = [exclude]
    }

    void setExclude(List<String> exclude) {
        this.exclude = exclude
    }
}
