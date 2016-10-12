package com.craigburke.gradle.client.registry.core

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import groovy.transform.CompileStatic

/**
 *
 * Resolver interface
 *
 * @author Craig Burke
 */
@CompileStatic
interface Resolver {
    boolean canResolve(Dependency dependency)
    List<Version> getVersionList(Dependency dependency)
    void resolve(Dependency dependency)
    void afterInfoLoad(Dependency dependency)
}
