package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import org.ajoberstar.grgit.Grgit
import spock.lang.Unroll

class BowerRegistrySpec extends AbstractRegistrySpec {

    final static String GIT_URL_PLACEHOLDER = '$GIT_URL_BASE'

    def setup() {
        init(BowerRegistry, 'bower')
        String gitUrl = "file://${resource('bower').path}"

        mockResponses = [
                '/bower/packages/foo'      : resource('bower/packages/foo').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/bower/packages/bar'      : resource('bower/packages/bar').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/bower/packages/baz'      : resource('bower/packages/baz').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/bower/packages/foobar'   : resource('bower/packages/foobar').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/bower/packages/circular1': resource('bower/packages/circular1').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/bower/packages/circular2': resource('bower/packages/circular2').text.replace(GIT_URL_PLACEHOLDER, gitUrl)
        ]
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        DeclaredDependency simpleDependency = new DeclaredDependency(name: name, versionExpression: version)
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        when:
        File source = registry.getSourceFolder(dependency)

        then:
        source.name == version

        and:
        source.parentFile.name == 'source'

        and:
        Grgit.open(dir: source.absolutePath).head().id == commitHash

        where:
        name  | version | commitHash
        'foo' | '1.0.0' | '563d901652d46ba84a3257f9c35997adbd350e6e'
        'bar' | '1.0.0' | 'd24b2c55280b9f048c6af770d9a57195b6e70ecb'
    }

}
