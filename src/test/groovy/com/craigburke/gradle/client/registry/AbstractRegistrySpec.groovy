package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version
import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.security.MessageDigest

import static com.github.tomakehurst.wiremock.client.WireMock.*

abstract class AbstractRegistrySpec extends Specification {

    static final int PROXY_PORT = 9999
    static WireMockServer httpMock = new WireMockServer(9999)
    private String resourceFolder

    @Subject
    Registry registry

    @Rule
    TemporaryFolder cacheFolder = new TemporaryFolder()

    @Rule
    TemporaryFolder installFolder = new TemporaryFolder()

    URL resource(String path) {
        AbstractRegistrySpec.classLoader.getResource("__files/${path}")
    }

    void init(Class<Registry> clazz, String resourceFolder) {
        registry = clazz.newInstance(["http://www.example.com/${resourceFolder}"] as Object[])
        registry.cachePath = cacheFolder.root.absolutePath
        registry.installPath = installFolder.root.absolutePath
        this.resourceFolder = resourceFolder
    }

    def setup() {
        httpMock.start()

        System.properties['http.proxyHost'] = 'localhost'
        System.properties['http.proxyPort'] = PROXY_PORT as String
    }

    void setMockResponses(Map<String, Object> mockedResponses) {
        mockedResponses.each { String url, content ->
            def response = aResponse().withStatus(200).withBody(content)
            httpMock.stubFor(get(urlEqualTo(url)).willReturn(response))
        }
    }

    def cleanup() {
        httpMock.stop()
    }

    String getChecksum(byte[] data) {
        MessageDigest.getInstance("SHA1")
                .digest(data)
                .collect { byte part -> String.format("%02x", part) }
                .join('')
    }

    @Unroll
    def "get version list for #name"() {
        setup:
        SimpleDependency dependency = new SimpleDependency(name: name, versionExpression: '1.0.0')

        expect:
        registry.getVersionList(dependency).sort() == Version.toList(versions)

        where:
        name  | versions
        'foo' | ['1.0.0', '1.1.0', '1.1.1', '1.2.0', '2.0.0']
        'bar' | ['0.0.1', '1.0.0', '2.0.0']
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
    def "can load #name@#version with exclusions"() {
        given:
        SimpleDependency simpleDependency = new SimpleDependency(name: name, versionExpression: version, excludes: exclusions)

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
        name     | version | exclusions | children
        'foo'    | '1.0.0' | ['bar']    | ['baz@1.0.0']
        'foo'    | '1.0.0' | ['baz']    | ['bar@1.0.0']
        'foobar' | '1.0.0' | ['foo']    | []
    }

    @Unroll
    def "can load #name@#version without transitive dependencies"() {
        given:
        SimpleDependency simpleDependency = new SimpleDependency(name: name, versionExpression: version, transitive: false)

        when:
        Dependency dependency = registry.loadDependency(simpleDependency)
        List<Dependency> childDependencies = Dependency.flattenList(dependency.children)

        then:
        dependency.name == name

        and:
        dependency.version.fullVersion == version

        and:
        childDependencies == []

        where:
        name  | version
        'foo' | '1.0.0'
        'foo' | '1.0.0'
    }

    def "can load module directly from git repo"() {
        given:
        String gitRepoUrl = "file://${resource(resourceFolder).path}/foo-git.git"
        SimpleDependency simpleDependency = new SimpleDependency(name: 'foo-git', versionExpression: '1.0.0', url: gitRepoUrl)

        when:
        Dependency dependency = registry.loadDependency(simpleDependency)

        then:
        dependency.name == 'foo-git'

        and:
        dependency.version.fullVersion == '1.0.0'
    }


}
