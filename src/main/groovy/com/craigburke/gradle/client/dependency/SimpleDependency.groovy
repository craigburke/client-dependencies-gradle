package com.craigburke.gradle.client.dependency

class SimpleDependency {
    String name
    String versionExpression
    String url

    List<String> excludes = []
    boolean transitive = true
}
