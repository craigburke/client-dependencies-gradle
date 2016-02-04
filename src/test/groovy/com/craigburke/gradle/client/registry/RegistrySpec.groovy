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

    @Unroll
    def "#path from #source to #destination resolves to #result"() {
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

    @Unroll
    def "#path from #source (prefix: #prefix) to #destination resolves to #result"() {
        setup:
        mockRegistry.sourcePathPrefix = prefix

        expect:
        mockRegistry.getDestinationPath(path, source, destination) == result

        where:
        path                    | prefix     | source          | destination | result
        'foo/source.js'         | 'foo/'     | '**'            | '/foo/'     | '../foo/source.js'
        'foo/source.js'         | 'foo/'     | '**'            | 'foo/'      | 'foo/source.js'
        'foo/source.js'         | 'foo/'     | '**'            | 'foo'       | 'foo'
        'foo/css/style.css'     | 'foo/'     | 'css/style.css' | 'style/'    | 'style/style.css'
        'foo/foo/css/style.css' | 'foo/'     | 'foo/**'        | 'style/'    | 'style/css/style.css'
        'foo/foo/css/style.css' | 'foo/'     | 'foo/**'        | 'style'     | 'style'
        'foo/bar/style.css'     | 'foo/bar/' | 'foo/bar/**'    | 'baz/'      | 'baz/style.css'
    }

    @Unroll
    def "#sourceExpression (prefix: #prefix) resolves to #result"() {
        setup:
        mockRegistry.sourcePathPrefix = prefix

        expect:
        mockRegistry.getSourceIncludeExpression(sourceExpression) == result

        where:
        prefix | sourceExpression | result
        ''     | '**'             | '**'
        ''     | 'foo.js'         | 'foo.js'
        ''     | 'foo/foo.js'     | 'foo/foo.js'
        ''     | 'foo/**/foo.js'  | 'foo/**/foo.js'
        'foo/' | '**'             | 'foo/**'
        'foo/' | 'foo.js'         | 'foo/foo.js'
        'foo/' | 'foo/foo.js'     | 'foo/foo/foo.js'
        'foo/' | 'foo/**/foo.js'  | 'foo/foo/**/foo.js'
    }


    @Unroll
    def "main folder path for #dependency matches #result"() {
        setup:
        mockRegistry.cachePath = cachePath

        expect:
        mockRegistry.getMainFolderPath(dependencyName) == result

        where:
        cachePath  | dependencyName | result
        '/foo'     | 'foo'          | '/foo/foo'
        '/foo/bar' | 'foo'          | '/foo/bar/foo'
        '/'        | 'foo'          | '/foo'
        'C:\\foo'  | 'foo'          | 'C:/foo/foo'
    }

}
