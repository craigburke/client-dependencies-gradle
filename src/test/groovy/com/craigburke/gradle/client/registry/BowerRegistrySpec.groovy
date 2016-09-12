package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.bower.BowerRegistry
import com.craigburke.gradle.client.registry.bower.GitResolver
import com.craigburke.gradle.client.registry.bower.GithubResolver
import org.ajoberstar.grgit.Grgit
import spock.lang.Unroll

class BowerRegistrySpec extends AbstractRegistrySpec {

    final static String GIT_URL_PLACEHOLDER = '$GIT_URL_BASE'

    def setup() {
        init(BowerRegistry, 'bower')
        String gitUrl = "file://${resource('bower').path}"

        Map responses = ['foo', 'bar', 'baz', 'foobar', 'circular1', 'circular2', 'dotbower', 'notag'].collectEntries {
            String url = "/bower/packages/${it}" as String
            String response = resource("bower/packages/${it}").text.replace(GIT_URL_PLACEHOLDER, gitUrl)
            [(url) : response]
        }

        mockResponses(responses)
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        Dependency simpleDependency = new Dependency(name: name,
                baseSourceDir: sourceFolder,
                versionExpression: version)

        when:
        Dependency dependency = registry.loadDependency(simpleDependency, null)
        File source = dependency.sourceDir

        then:
        Grgit.open(dir: source.absolutePath).head().id == commitHash

        where:
        name  | version | commitHash
        'foo' | '1.0.0' | '563d901652d46ba84a3257f9c35997adbd350e6e'
        'bar' | '1.0.0' | 'd24b2c55280b9f048c6af770d9a57195b6e70ecb'
    }

    @Unroll
    def "correct resolver is used for dependency with url #url"() {
        given:
        Dependency dependency = new Dependency(name: 'foo',
                registry: registry, url: url)

        expect:
        registry.getResolver(dependency).getClass() == resolverClass

        where:
        url                                  | resolverClass
        'http://www.example.com/foo/bar.git' | GitResolver
        'http://www.github.com/foo/bar.git'  | GithubResolver
    }

    def "can handle repository with a .bower.json file"() {
        given:
        Dependency simpleDependency = new Dependency(name: 'dotbower',
                baseSourceDir: sourceFolder,
                versionExpression: '1.0.0')

        when:
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        then:
        dependency.children*.name == ['foo']
    }

    def "git repo with no tags can still resolve a single version"() {
        given:
        Dependency simpleDependency = new Dependency(
                name: 'notag',
                baseSourceDir: sourceFolder,
                versionExpression: '*')

        when:
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        then:
        notThrown(Exception)

        and:
        dependency.version.fullVersion == '1.0.0'
    }

}
