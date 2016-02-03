package com.craigburke.gradle.client.registry

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RegistrySpec extends Specification {

    @Shared
    Registry mockRegistry

    def setup() {
        mockRegistry = [:] as Registry
    }

    @Unroll('#path from #source to #destination resolves to #result')
    def "can calculate destination path"() {
        expect:
        mockRegistry.getDestinationPath(path, source, destination) == result

        where:
        path                | source          | destination | result
        'source.js'         | '**'            | '/foo/'     | '../foo/source.js'
        'source.js'         | '**'            | 'foo/'      | 'foo/source.js'
        'source.js'         | '**'            | 'foo'       | 'foo'
        'css/style.css'     | 'css/style.css' | 'style/'    | 'style/style.css'
        'foo/css/style.css' | 'foo/**'        | 'style/'    | 'style/css/style.css'
        'foo/css/style.css' | 'foo/**'        | 'style'     | 'style'
    }

}
