package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.npm.NpmRegistry
import spock.lang.Unroll

class NpmRegistrySpec extends AbstractRegistrySpec {

    def setup() {
        init(NpmRegistry, 'npm')
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        Dependency simpleDependency = new Dependency(name: name, sourceFolder: sourceFolder, versionExpression: version)
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        when:
        File source = dependency.sourceFolder

        then:
        new File("${source.absolutePath}/${name}.js").exists()

        where:
        name  | version
        'foo' | '1.0.0'
        'bar' | '1.0.0'
    }

    def "peer dependencies are included"() {
        given:
        Dependency simpleDependency = new Dependency(name: 'peer', sourceFolder: sourceFolder, versionExpression: '1.0.0')
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        when:
        Dependency loadedDependency = registry.loadDependency(dependency, null)

        then:
        loadedDependency.children*.toString().sort() == ['bar@1.0.0', 'foo@1.0.0']
    }

}
