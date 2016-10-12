package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.registry.core.AbstractRegistry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RegistryBaseSpec extends Specification {

    @Shared AbstractRegistry registryBase

    def setup() {
        String url = 'http://www.example.com'
        registryBase = new TestRegistryBase('test', url, [])
    }

    @Unroll
    def "main folder path for #dependency matches #result"() {
        setup:
        registryBase.localCacheDir = new File(cachePath)

        expect:
        registryBase.getMainFolderPath(dependencyName) == result

        where:
        cachePath  | dependencyName | result
        '/foo'     | 'foo'          | '/foo/foo'
        '/foo/bar' | 'foo'          | '/foo/bar/foo'
        '/'        | 'foo'          | '/foo'
    }

}
