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

}
