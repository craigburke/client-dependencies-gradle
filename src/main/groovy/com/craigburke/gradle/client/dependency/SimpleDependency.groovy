package com.craigburke.gradle.client.dependency

class SimpleDependency {
    String name
    String versionExpression
    List<String> excludes = []
    boolean transitive = true

    boolean isGitDependency() {
        ['http://', 'https://', 'file://', 'ssh://', 'git://'].any { versionExpression.startsWith(it) }
    }

}
