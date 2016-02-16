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
