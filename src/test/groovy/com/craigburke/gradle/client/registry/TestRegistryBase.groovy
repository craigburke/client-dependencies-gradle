package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.RegistryBase
import com.craigburke.gradle.client.registry.core.Resolver
import org.gradle.api.logging.Logger

class TestRegistryBase extends RegistryBase {

    TestRegistryBase(String url, Logger log, List<Class<Resolver>> resolvers) {
        super(url, log, resolvers)
    }

    Registry getRegistry() { return null }
    String getDependencyUrl(Dependency dependency) { return null }
    boolean downloadDependencyFromCache(Dependency dependency) { return false }
    List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) { return null }
}
