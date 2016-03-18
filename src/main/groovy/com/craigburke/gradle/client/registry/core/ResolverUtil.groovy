package com.craigburke.gradle.client.registry.core

import groovy.transform.CompileStatic

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 *
 * Util class for all resolvers
 *
 * @author Craig Burke
 */
@CompileStatic
class ResolverUtil {
    private static final ConcurrentMap<String, Object> DEPENDENCY_LOCKS = [:] as ConcurrentHashMap

    static withLock(String key, Closure closure) {
        DEPENDENCY_LOCKS.putIfAbsent(key, new Object())
        Object lock = DEPENDENCY_LOCKS[key]
        synchronized(lock) {
            closure()
        }
    }

    static String getMD5Hash(String input) {
        MessageDigest.getInstance('MD5').digest(input.bytes).encodeHex().toString()
    }

}
