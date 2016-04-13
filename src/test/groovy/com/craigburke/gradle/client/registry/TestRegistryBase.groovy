package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
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

    TestRegistryBase(String name, String url, Logger log, List<Class<Resolver>> resolvers) {
        super(name, url, log, resolvers)
    }

    boolean loadSourceFromGlobalCache(Dependency dependency) { false }
    List<SimpleDependency> getChildDependencies(Dependency dependency) { null }
    Map loadInfoFromGlobalCache(Dependency dependency) { null }
    Map loadInfoFromRegistry(Dependency dependency) { null }
}
