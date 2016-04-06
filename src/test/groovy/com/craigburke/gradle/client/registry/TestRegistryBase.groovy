package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.core.AbstractRegistry
import com.craigburke.gradle.client.registry.core.Resolver
import org.gradle.api.logging.Logger

/**
 *
 * Minimal AbstractRegistry implementation for testing
 *
 * @author Craig Burke
 */
class TestRegistryBase extends AbstractRegistry {

    TestRegistryBase(String url, Logger log, List<Class<Resolver>> resolvers) {
        super(url, log, resolvers)
    }

    String getDependencyUrl(Dependency dependency) { null }
    boolean downloadDependencyFromCache(Dependency dependency) { false }
    List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) { null }
    Map loadInfoFromGlobalCache(Dependency dependency) { null }
    Map loadInfoFromRegistry(Dependency dependency) { null }
}
