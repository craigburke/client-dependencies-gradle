package com.craigburke.gradle.client.registry.core

import com.craigburke.gradle.client.dependency.Dependency
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 *
 * Base class for all resolvers
 *
 * @author Craig Burke
 */
@CompileStatic
class ResolverBase {
    private static final ConcurrentMap<String, Object> DEPENDENCY_LOCKS = [:] as ConcurrentHashMap

    static withLock(Dependency dependency, Closure closure) {
        DEPENDENCY_LOCKS.putIfAbsent(dependency.name, new Object())
        Object lock = DEPENDENCY_LOCKS[dependency.name]
        synchronized(lock) {
            closure()
        }
    }

}
