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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

abstract class AbstractRegistrySpec extends Specification {

    static final int PROXY_PORT = 9999
    static WireMockServer httpMock = new WireMockServer(PROXY_PORT)

    @Subject
    Registry registry

    @Rule
    TemporaryFolder cacheFolder = new TemporaryFolder()

    @Rule
    TemporaryFolder installFolder = new TemporaryFolder()

    URL resource(String path) {
        AbstractRegistrySpec.classLoader.getResource(path)
    }

    void setupRegistry(Class<Registry> clazz) {
        registry = clazz.newInstance(['http://www.example.com'] as Object[])
        registry.cachePath = cacheFolder.root.absolutePath
        registry.installPath = installFolder.root.absolutePath
    }

    def setup() {
        httpMock.start()
        System.properties['http.proxyHost'] = 'localhost'
        System.properties['http.proxyPort'] = PROXY_PORT as String
    }

    def cleanup() {
        httpMock.stop()
    }

    void setResponses( Map<String, Object> mockedResponses) {
        mockedResponses.each { String url, content ->
            def response = aResponse().withStatus(200).withBody(content)
            httpMock.stubFor(get(urlEqualTo(url)).willReturn(response))
        }
    }

    String getChecksum(byte[] data) {
        MessageDigest.getInstance("SHA1")
                .digest(data)
                .collect { byte part -> String.format("%02x", part) }
                .join('')
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

}
