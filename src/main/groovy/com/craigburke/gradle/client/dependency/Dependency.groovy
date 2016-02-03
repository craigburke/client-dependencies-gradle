package com.craigburke.gradle.client.dependency

class Dependency {
    String name
    Version version
    String downloadUrl

    List<Dependency> children = []
}
