package com.craigburke.gradle.client.registry

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RegistryBaseSpec extends Specification {

    @Shared RegistryBase registryBase

    def setup() {
        registryBase = new RegistryBase()
    }

    @Unroll
    def "#path from #source to #destination resolves to #result"() {
        expect:
        registryBase.getDestinationPath(path, source, destination) == result

        where:
        path                | source              | destination | result
        'source.js'         | '**'                | '/foo/'     | '../foo/source.js'
        'source.js'         | '**'                | 'foo/'      | 'foo/source.js'
        'source.js'         | '**'                | 'foo'       | 'foo'
        'css/style.css'     | 'css/style.css'     | 'style/'    | 'style/style.css'
        'foo/css/style.css' | 'foo/**'            | 'style/'    | 'style/css/style.css'
        'foo/css/style.css' | 'foo/**'            | 'style'     | 'style'

        'foo/source.js'     | '**'                | '/foo/'     | '../foo/source.js'
        'foo/source.js'     | '**'                | 'foo/'      | 'foo/source.js'
        'foo/source.js'     | '**'                | 'foo'       | 'foo'

        'foo/css/style.css' | 'foo/css/style.css' | 'style/'    | 'style/style.css'
        'foo/css/style.css' | 'foo/**'            | 'style/'    | 'style/css/style.css'
        'foo/css/style.css' | 'foo/**'            | 'style'     | 'style'
        'foo/css/style.css' | 'foo/**'            | ''          | 'css/style.css'
        'foo/bar/style.css' | 'foo/bar/**'        | 'baz/'      | 'baz/style.css'
    }

    @Unroll
    def "#sourceExpression (prefix: #prefix) resolves to #result"() {
        setup:
        registryBase.sourcePathPrefix = prefix

        expect:
        registryBase.getSourceIncludeExpression(sourceExpression) == result

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
        registryBase.cachePath = cachePath

        expect:
        registryBase.getMainFolderPath(dependencyName) == result

        where:
        cachePath  | dependencyName | result
        '/foo'     | 'foo'          | '/foo/foo'
        '/foo/bar' | 'foo'          | '/foo/bar/foo'
        '/'        | 'foo'          | '/foo'
        'C:\\foo'  | 'foo'          | 'C:/foo/foo'
    }

}
