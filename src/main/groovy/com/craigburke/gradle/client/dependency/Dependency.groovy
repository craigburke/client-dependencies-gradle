package com.craigburke.gradle.client.dependency

import com.craigburke.gradle.client.registry.Registry

class Dependency {
    String name
    Version version
    String downloadUrl
    Registry registry

    List<Dependency> children = []
}
