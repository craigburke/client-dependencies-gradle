package com.craigburke.gradle.client.registry

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.AbstractRegistry

import com.craigburke.gradle.client.dependency.Dependency
import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import java.security.MessageDigest

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

    protected File getSourceFolder() {
        new File("${installFolder.newFolder().absolutePath}/source/")
    }

    void init(Class<Registry> clazz, String resourceFolder) {
        AbstractRegistry.threadPoolSize = 15
        registry = clazz.newInstance(['test', "http://www.example.com/${resourceFolder}"] as Object[])
        registry.localCacheDir = cacheFolder.root
        registry.installDir = installFolder.root
        this.resourceFolder = resourceFolder
    }

    def setup() {
        httpMock.start()

        System.properties['http.proxyHost'] = 'localhost'
        System.properties['http.proxyPort'] = PROXY_PORT as String
    }

    void mockResponses(Map<String, Object> mockedResponses) {
        mockedResponses.each { String url, content ->
            def response = aResponse().withStatus(200).withBody(content)
            httpMock.stubFor(get(urlEqualTo(url)).willReturn(response))
        }
    }

    def cleanup() {
        httpMock.stop()
    }

    String getChecksum(byte[] data) {
        MessageDigest.getInstance('SHA1')
                .digest(data)
                .collect { byte part -> String.format('%02x', part) }
                .join('')
    }

    @Unroll
    def "can load #name@#version and child dependencies"() {
        given:
        Dependency declaredDependency = new Dependency(name: name, versionExpression: version)

        when:
        Dependency dependency = registry.loadDependency(declaredDependency, null)
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
        Dependency declaredDependency = new Dependency(name: name,
                baseSourceDir: sourceFolder,
                versionExpression: version,
                exclude: exclude)

        when:
        Dependency dependency = registry.loadDependency(declaredDependency, null)
        List<Dependency> childDependencies = Dependency.flattenList(dependency.children)

        then:
        dependency.name == name

        and:
        dependency.version.fullVersion == version

        and:
        childDependencies.collect { "${it.name}@${it.version}" as String } == children

        where:
        name     | version | exclude | children
        'foo'    | '1.0.0' | ['bar'] | ['baz@1.0.0']
        'foo'    | '1.0.0' | ['baz'] | ['bar@1.0.0']
        'foobar' | '1.0.0' | ['foo'] | []
    }

    @Unroll
    def "can load #name@#version without transitive dependencies"() {
        given:
        Dependency declaredDependency = new Dependency(
                name: name,
                versionExpression: version,
                baseSourceDir: sourceFolder,
                transitive: false
        )

        when:
        Dependency dependency = registry.loadDependency(declaredDependency, null)
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
        String url = "file://${resource(resourceFolder).path}/foo-git.git"
        Dependency declaredDependency = new Dependency(name: 'foo-git', versionExpression: '1.0.0', url: url)

        when:
        Dependency dependency = registry.loadDependency(declaredDependency, null)

        then:
        dependency.name == 'foo-git'

        and:
        dependency.children*.name == ['bar', 'baz']

        and:
        dependency.version.fullVersion == '1.0.0'
    }

    def "parent dependencies are assigned correctly"() {
        setup:
        Dependency declaredDependency = new Dependency(name: 'foo', versionExpression: '1.0.0')

        when:
        Dependency dependency = registry.loadDependency(declaredDependency, null)

        then:
        dependency.parent == null

        and:
        dependency.children.every { it.parent == dependency }
    }

    def "circular dependencies are ignored"() {
        setup:
        Dependency declaredDependency = new Dependency(name: 'circular1', versionExpression: '1.0.0')

        when:
        Dependency circular1 = registry.loadDependency(declaredDependency, null)
        Dependency circular2 = circular1.children.first()

        then:
        circular1.children.size() == 1

        and:
        circular2.name == 'circular2'

        and:
        circular2.children.size() == 0
    }

}
