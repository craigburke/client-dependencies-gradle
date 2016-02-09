package com.craigburke.gradle.client.registry

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject

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

    void setBinaryResponses( Map<String, String> mockedResponses) {
        mockedResponses.each { String url, String resource ->
            byte[] data = AbstractRegistrySpec.classLoader.getResource(resource).bytes
            def response = aResponse().withStatus(200).withBody(data)
            httpMock.stubFor(get(urlEqualTo(url)).willReturn(response))
        }
    }

    String getChecksum(byte[] data) {
        MessageDigest.getInstance("SHA1")
                .digest(data)
                .collect { byte part -> String.format("%02x", part) }
                .join('')
    }

}
