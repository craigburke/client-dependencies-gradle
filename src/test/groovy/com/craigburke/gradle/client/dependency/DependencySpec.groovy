package com.craigburke.gradle.client.dependency

import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class DependencySpec extends Specification {

    def "ancestors can be determined"() {
        given:
        Dependency dependency1 = new Dependency(name: 'dependency1')
        Dependency dependency2 = new Dependency(name: 'dependency2', parent: dependency1)
        Dependency dependency3 = new Dependency(name: 'dependency3', parent: dependency2)

        expect:
        dependency1.ancestorsAndSelf == [dependency1]

        and:
        dependency2.ancestorsAndSelf.sort { it.name } == [dependency1, dependency2]

        and:
        dependency3.ancestorsAndSelf.sort { it.name } == [dependency1, dependency2, dependency3]
    }

    @Unroll
    def "relative path for #name is #relativePath"() {
        setup:
        Dependency dependency = new Dependency(name: name, into: into)

        expect:
        dependency.destinationPath == destinationPath

        where:
        name       | into       | destinationPath
        'foo'      | null       | 'foo'
        'foo bar'  | null       | 'foo-bar'
        '@foo/bar' | null       | '@foo/bar'
        'foo'      | 'foo2'     | 'foo2'
        'foo bar'  | 'foo-bar2' | 'foo-bar2'
        '@foo/bar' | 'foo-bar2' | 'foo-bar2'
    }

    @Unroll
    def "from property is correct resolves to #releaseFolder"() {
        setup:
        TemporaryFolder baseSourceFolder = new TemporaryFolder()
        baseSourceFolder.create()

        Version version = Version.parse('1.0.0')
        Dependency dependency = new Dependency(name: 'foo', versionExpression: '1.0.0',
                version: version, from: from, baseSourceDir: baseSourceFolder.root)

        subfolders.each { new File("${dependency.sourceDir.absolutePath}/${it}").mkdirs() }

        expect:
        dependency.getReleaseFolder(releaseFolders) == releaseFolder

        where:
        subfolders     | releaseFolders | from  | releaseFolder
        []             | []             | null  | ''
        ['foo']        | []             | null  | ''
        ['foo']        | ['foo']        | null  | 'foo'
        []             | []             | 'bar' | 'bar'
        ['foo']        | ['foo']        | 'bar' | 'bar'
        ['foo', 'bar'] | ['foo', 'bar'] | null  | 'foo'
    }

    @Unroll
    def "url property set to #url returns #expectedUrl"() {
        setup:
        Dependency dependency = new Dependency(name: 'foo', url: url)

        expect:
        dependency.fullUrl == expectedUrl

        where:
        url                             | expectedUrl
        'https://github.com/foo/bar'    | 'https://github.com/foo/bar'
        'http://example.com/foobar.git' | 'http://example.com/foobar.git'
        'foo/bar'                       | 'https://github.com/foo/bar'
        'file:///foo/bar'               | 'file:///foo/bar'
    }

    @Unroll
    def "can parse version expression #versionExpression"() {
        setup:
        Dependency dependency = new Dependency(name: 'foo', versionExpression: versionExpression)

        expect:
        dependency.fullUrl == expectedUrl

        and:
        dependency.versionExpression == expectedVersion

        where:
        versionExpression      | expectedVersion | expectedUrl
        'org/repo'             | ''              | 'https://github.com/org/repo'
        'org/repo#1.0.0'       | '1.0.0'         | 'https://github.com/org/repo'
        'org/repo#254e400'     | ''              | 'https://github.com/org/repo#254e400'
        'org/repo#development' | ''              | 'https://github.com/org/repo#development'
        'org/repo@1.0.0'       | '1.0.0'         | 'https://github.com/org/repo'
    }

}
