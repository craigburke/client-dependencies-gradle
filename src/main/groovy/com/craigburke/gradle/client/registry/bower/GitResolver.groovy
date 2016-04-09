package com.craigburke.gradle.client.registry.bower

import static com.craigburke.gradle.client.registry.core.RegistryUtil.withLock

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
        withLock(dependency.name) {
            Grgit repo = getRepository(dependency)
            repo.tag.list().collect { Version.parse(it.name as String) }
        } as List<Version>
    }

    void downloadDependency(Dependency dependency) {
        withLock(dependency.key) {
            Grgit repo = getRepository(dependency)
            String commit = repo.tag.list().find { (it.name - 'v') == dependency.version.fullVersion }.commit.id
            repo.reset(commit: commit, mode: ResetOp.Mode.HARD)
        }
    }

    private Grgit getRepository(Dependency dependency) {
        File sourceFolder = dependency.sourceDir

        if (sourceFolder.exists()) {
            Grgit.open(dir: sourceFolder.absolutePath)
        }
        else {
            String gitUrl = dependency.url ?: dependency.registry.loadInfo(dependency).url
            Grgit.clone(dir: sourceFolder, uri: gitUrl)
        }
    }
}
