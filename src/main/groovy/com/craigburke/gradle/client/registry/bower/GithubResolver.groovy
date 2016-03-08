package com.craigburke.gradle.client.registry.bower

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import com.craigburke.gradle.client.registry.core.ResolverBase
import groovy.json.JsonSlurper
import java.util.regex.Pattern
import org.gradle.api.logging.Logger

/**
 *
 * Github specific resolver for Bower
 *
 * @author Craig Burke
 */
class GithubResolver extends ResolverBase implements Resolver {

    private final Logger log

    GithubResolver(Logger log) {
        this.log = log
    }

    class GithubInfo {
        String orgName
        String repoName
    }

    static final String GITHUB_BASE_URL = 'https://api.github.com/repos'
    static final Pattern GITHUB_URL = ~/.*github\.com\/(.*)\/(.*)(?:\.git)/

    boolean canResolve(Dependency dependency) {
        dependency.url?.matches(GITHUB_URL)
    }

    private GithubInfo getInfo(String url) {
        GithubInfo info
        url.find(GITHUB_URL) { String match, String orgName, String repoName ->
            info = new GithubInfo(orgName: orgName, repoName: repoName)
        }
        info
    }

    private File getDownloadFile(Dependency dependency) {
        new File("${dependency.sourceFolder.parentFile.absolutePath}/${dependency.version.fullVersion}.tar.gz")
    }

    List<Version> getVersionList(Dependency dependency) {
        withLock(dependency.name) {
            GithubInfo info = getInfo(dependency.url)

            URL url = new URL("${GITHUB_BASE_URL}/${info.orgName}/${info.repoName}/git/refs/tags")
            new JsonSlurper().parse(url).collect { Version.parse((it.ref as String) - 'refs/tags/') }
        } as List<Version>
    }

    void downloadDependency(Dependency dependency) {
        withLock(dependency.key) {
            if (dependency.sourceFolder.exists()) {
                return
            }

            GithubInfo info = getInfo(dependency.url)
            URL url = new URL("https://github.com/${info.orgName}/${info.repoName}/archive/v${dependency.version}.tar.gz")

            File downloadFile = getDownloadFile(dependency)
            downloadFile.parentFile.mkdirs()

            downloadFile.withOutputStream { out ->
                out << url.openStream()
            }

            AntBuilder builder = new AntBuilder()
            builder.project.buildListeners.first().setMessageOutputLevel(0)

            builder.untar(src: downloadFile.absolutePath, dest: dependency.sourceFolder.absolutePath,
                    compression: 'gzip', overwrite: true)

            dependency.sourceFolder.listFiles().each { File file ->
                if (file.directory) {
                    builder.copy(todir: dependency.sourceFolder.absolutePath) {
                        fileSet(dir: file.absolutePath)
                    }
                    file.deleteDir()
                }
                else {
                    file.delete()
                }
            }

            downloadFile.delete()
        }
    }

}
