package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Subject
import spock.lang.Unroll

class NpmRegistrySpec extends HttpBaseSpec {

    @Subject Registry registry = new NpmRegistry("http://www.example.com")
    @Rule TemporaryFolder cacheFolder = new TemporaryFolder()
    @Rule TemporaryFolder installFolder = new TemporaryFolder()

    def setup() {
        registry.cachePath = cacheFolder.root.absolutePath
        registry.installPath = installFolder.root.absolutePath

        setHttpResponses([
                '/foo': 'npm/foo.json',
                '/bar': 'npm/bar.json',
                '/baz': 'npm/baz.json'
        ])
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
        List<String> childDependencies = dependency.children.collect { "${it.name}@${it.version.fullVersion}" as String }

        then:
        dependency.name == name

        and:
        dependency.version.fullVersion == version

        and:
        childDependencies == children

        where:
        name  | version | children
        'foo' | '1.0.0' | ['bar@1.0.0', 'baz@1.0.0']
        'foo' | '2.0.0' | []
    }


}
