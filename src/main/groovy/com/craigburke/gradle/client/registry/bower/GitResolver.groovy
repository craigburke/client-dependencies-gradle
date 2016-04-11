package com.craigburke.gradle.client.registry.bower

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.ResetOp
import org.gradle.api.logging.Logger

/**
 *
 * General git resolver for bower
 *
 * @author Craig Burke
 */
class GitResolver implements Resolver {

    private final Logger log

    GitResolver(Logger log) {
        this.log = log
    }

    boolean canResolve(Dependency dependency) {
        (dependency != null)
    }

    List<Version> getVersionList(Dependency dependency) {
        Grgit repo = getRepository(dependency)
        repo.tag.list().collect { Version.parse(it.name as String) }
    }

    void loadSource(Dependency dependency) {
        Grgit repo = getRepository(dependency)
        String commit = repo.tag.list().find { (it.name - 'v') == dependency.version.fullVersion }.commit.id
        repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
    }

    private Grgit getRepository(Dependency dependency) {
        File sourceFolder = dependency.sourceDir
        String gitUrl = dependency.url ?: dependency.registry.loadInfo(dependency).url
        Grgit.clone(dir: sourceFolder, uri: gitUrl)

    }
}
