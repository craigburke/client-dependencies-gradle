package com.craigburke.gradle.client.registry.bower

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.Version
import com.craigburke.gradle.client.registry.core.Resolver
import groovy.json.JsonSlurper

import java.util.regex.Pattern
import org.gradle.api.logging.Logger

/**
 *
 * Github specific resolver for Bower
 *
 * @author Craig Burke
 */
class GithubResolver implements Resolver {

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

    @Override
    boolean canResolve(Dependency dependency) {
        dependency.fullUrl?.matches(GITHUB_URL)
    }

    @Override
    List<Version> getVersionList(Dependency dependency) {
        if (dependency.info.tags) {
            dependency.info.tags?.collect { String tag -> Version.parse(tag) } as List<Version>
        }
        else {
            []
        }
    }

    @Override
    void resolve(Dependency dependency) {
        if (dependency.sourceDir.listFiles()) {
            return
        }

        GithubInfo info = getInfo(dependency.fullUrl)
        String ref = dependency.info.tags.find { (it - 'v') == dependency.version.fullVersion } ?: 'master'
        URL url = new URL("${GITHUB_BASE_URL}/${info.orgName}/${info.repoName}/tarball/${ref}")

        File downloadFile = getDownloadFile(dependency)
        downloadFile.parentFile.mkdirs()

        downloadFile.withOutputStream { out ->
            out << url.openStream()
        }

        AntBuilder builder = new AntBuilder()
        builder.project.buildListeners.first().setMessageOutputLevel(0)

        builder.untar(src: downloadFile.absolutePath, dest: dependency.sourceDir.absolutePath,
                compression: 'gzip', overwrite: true)

        dependency.sourceDir.listFiles().each { File file ->
            if (file.directory) {
                builder.copy(todir: dependency.sourceDir.absolutePath) {
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

    @Override
    void afterInfoLoad(Dependency dependency) {
        if (!dependency.info.tags) {
            GithubInfo info = getInfo(dependency.fullUrl)
            URL url = new URL("${GITHUB_BASE_URL}/${info.orgName}/${info.repoName}/git/refs/tags")
            List<String> tags = new JsonSlurper().parse(url).collect { (it.ref as String) - 'refs/tags/' }
            dependency.info.tags = tags
        }
    }

    @Override
    Version getVersionFromSource(Dependency dependency) {
        BowerConfig.getVersion(dependency.sourceDir)
    }

    private GithubInfo getInfo(String url) {
        GithubInfo info
        url.find(GITHUB_URL) { String match, String orgName, String repoName ->
            info = new GithubInfo(orgName: orgName, repoName: repoName)
        }
        info
    }

    private static File getDownloadFile(Dependency dependency) {
        new File("${dependency.baseSourceDir.absolutePath}/${dependency.key}.tar.gz")
    }

}
