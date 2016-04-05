package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.core.RegistryBase
import com.craigburke.gradle.client.registry.core.Resolver
import org.gradle.api.logging.Logger

/**
 *
 * Minimal RegistryBase implementation for testing
 *
 * @author Craig Burke
 */
class TestRegistryBase extends RegistryBase {

    TestRegistryBase(String url, Logger log, List<Class<Resolver>> resolvers) {
        super(url, log, resolvers)
    }

    String getDependencyUrl(Dependency dependency) { null }
    boolean downloadDependencyFromCache(Dependency dependency) { false }
    List<Dependency> loadChildDependencies(Dependency dependency, List<String> exclusions) { null }
}
