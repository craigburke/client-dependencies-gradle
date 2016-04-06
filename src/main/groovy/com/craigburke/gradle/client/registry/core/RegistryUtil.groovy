package com.craigburke.gradle.client.registry.core

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 *
 * Util class for registries
 *
 * @author Craig Burke
 */
class RegistryUtil {
    private static final ConcurrentMap<String, Object> DEPENDENCY_LOCKS = [:] as ConcurrentHashMap

    static withLock(String key, Closure closure) {
        DEPENDENCY_LOCKS.putIfAbsent(key, new Object())
        Object lock = DEPENDENCY_LOCKS[key]
        synchronized(lock) {
            closure()
        }
    }

    static String getMD5Hash(String input) {
        getMD5Hash(input.bytes)
    }

    static String getMD5Hash(byte[] input) {
        MessageDigest.getInstance('MD5').digest(input).encodeHex().toString()
    }

    static getShaHash(String input) {
        getShaHash(input.bytes)
    }

    static getShaHash(byte[] input) {
        MessageDigest.getInstance('SHA1').digest(input).encodeHex().toString()
    }
}
