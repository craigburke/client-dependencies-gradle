package com.craigburke.gradle.client.registry

import com.github.tomakehurst.wiremock.WireMockServer
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

abstract class HttpBaseSpec extends Specification {

    static final int PROXY_PORT = 9999
    static WireMockServer httpMock = new WireMockServer(PROXY_PORT)

    def setup() {
        httpMock.start()

        System.properties['http.proxyHost'] = 'localhost'
        System.properties['http.proxyPort'] = PROXY_PORT as String
    }

    def cleanup() {
        httpMock.stop()
    }

    void setHttpResponses( Map<String, String> mockedResponses) {
        mockedResponses.each { String url, String resource ->
            String content = HttpBaseSpec.classLoader.getResource(resource).text
            def response = aResponse().withStatus(200).withBody(content)

            httpMock.stubFor(get(urlEqualTo(url)).willReturn(response))
        }
    }

}
