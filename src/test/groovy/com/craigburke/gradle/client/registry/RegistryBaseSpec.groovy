package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.registry.core.AbstractRegistry
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RegistryBaseSpec extends Specification {

    @Shared AbstractRegistry registryBase

    def setup() {
        String url = 'http://www.example.com'
        Logger log = Logging.getLogger(AbstractRegistry)
        registryBase = new TestRegistryBase(url, log, [])
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
