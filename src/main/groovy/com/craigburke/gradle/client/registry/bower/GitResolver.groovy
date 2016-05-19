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

    @Override
    boolean canResolve(Dependency dependency) {
        (dependency != null)
    }

    @Override
    List<Version> getVersionList(Dependency dependency) {
        Grgit repo = Grgit.open(dir: dependency.sourceDir)
        repo.tag.list().collect { Version.parse(it.name as String) }
    }

    @Override
    void resolve(Dependency dependency) {
        Grgit repo = Grgit.open(dir: dependency.sourceDir)
        String commit = repo.tag.list().find { (it.name - 'v') == dependency.version.fullVersion }.commit.id
        repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
    }

    @Override
    void afterInfoLoad(Dependency dependency) {
        Grgit.clone(dir:  dependency.sourceDir, uri: dependency.fullUrl)
    }

}
