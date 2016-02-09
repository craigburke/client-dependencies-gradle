package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import spock.lang.Unroll

class NpmRegistrySpec extends AbstractRegistrySpec {

    def setup() {
        setupRegistry(NpmRegistry)

        responses = [
                '/foo'          : resource('npm/foo.json').text,
                '/bar'          : resource('npm/bar.json').text,
                '/baz'          : resource('npm/baz.json').text,
                '/foobar'       : resource('npm/foobar.json').text,
                '/foo-1.0.0.tgz': resource('npm/foo-1.0.0.tgz').bytes,
                '/bar-1.0.0.tgz': resource('npm/bar-1.0.0.tgz').bytes
        ]
    }

    @Unroll
    def "get version list for #dependency"() {
        expect:
        registry.getVersionList(dependency).sort() == Version.toList(versions)

        where:
        dependency | versions
        'foo'      | ['1.0.0', '1.1.0', '1.1.1', '1.2.0', '2.0.0']
        'bar'      | ['0.0.1', '1.0.0', '2.0.0']
    }

    @Unroll
    def "can load #name@#version and child dependencies"() {
        given:
        SimpleDependency simpleDependency = new SimpleDependency(name: name, versionExpression: version)

        when:
        Dependency dependency = registry.loadDependency(simpleDependency)
        List<Dependency> childDependencies = Dependency.flattenList(dependency.children)

        then:
        dependency.name == name

        and:
        dependency.version.fullVersion == version

        and:
        childDependencies.collect { "${it.name}@${it.version}" as String } == children

        where:
        name     | version | children
        'foo'    | '1.0.0' | ['bar@1.0.0', 'baz@1.0.0']
        'foo'    | '2.0.0' | []
        'foobar' | '1.0.0' | ['foo@1.0.0', 'bar@1.0.0', 'baz@1.0.0']
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        SimpleDependency simpleDependency = new SimpleDependency(name: name, versionExpression: version)
        Dependency dependency = registry.loadDependency(simpleDependency)

        when:
        File source = registry.getInstallSource(dependency)

        then:
        source.name == "package.tgz"

        and:
        getChecksum(source.bytes) == checksum

        where:
        name  | version | checksum
        'foo' | '1.0.0' | '117f23ed600939d08eb0cf258439c842c7feea2d'
        'bar' | '1.0.0' | 'f097c2eb1f6e27d34566ccabe689f25881a03558'
    }


}
