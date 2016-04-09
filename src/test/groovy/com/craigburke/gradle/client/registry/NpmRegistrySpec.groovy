package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.core.DownloadVerifyException
import com.craigburke.gradle.client.registry.npm.NpmRegistry
import spock.lang.Unroll

class NpmRegistrySpec extends AbstractRegistrySpec {

    static final String CHECKSUM_PLACEHOLDER = '$CHECKSUM'

    def setup() {
        init(NpmRegistry, 'npm')
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        Dependency simpleDependency = new Dependency(name: name, baseSourceDir: sourceFolder, versionExpression: version)
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        when:
        File source = dependency.sourceDir

        then:
        new File("${source.absolutePath}/${name}.js").exists()

        where:
        name  | version
        'foo' | '1.0.0'
        'bar' | '1.0.0'
    }

    def "peer dependencies are included"() {
        given:
        Dependency simpleDependency = new Dependency(name: 'peer', baseSourceDir: sourceFolder, versionExpression: '1.0.0')
        Dependency dependency = registry.loadDependency(simpleDependency, null)

        when:
        Dependency loadedDependency = registry.loadDependency(dependency, null)

        then:
        loadedDependency.children*.toString().sort() == ['bar@1.0.0', 'foo@1.0.0']
    }

    def "exception thrown when checksum is invalid"() {
        setup:
        mockResponses([
                '/npm/checksum' : resource('npm/checksum').text.replace(CHECKSUM_PLACEHOLDER, 'FOO')
        ])
        Dependency dependency = new Dependency(name: 'checksum', baseSourceDir: sourceFolder, versionExpression: '1.0.0')

        when:
        registry.loadDependency(dependency, null)

        then:
        thrown(DownloadVerifyException)
    }

    def "exception not thrown when checksum is valid"() {
        setup:
        String checksum = '5f446f27d62cd7f62eeaf776a825158df3d2b1d5'
        mockResponses([
                '/npm/checksum' : resource('npm/checksum').text.replace(CHECKSUM_PLACEHOLDER, checksum)
        ])
        Dependency dependency = new Dependency(name: 'checksum', baseSourceDir: sourceFolder, versionExpression: '1.0.0')

        when:
        registry.loadDependency(dependency, null)

        then:
        notThrown(DownloadVerifyException)
    }

}
