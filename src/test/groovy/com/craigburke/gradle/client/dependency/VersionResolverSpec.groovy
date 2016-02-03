package com.craigburke.gradle.client.dependency

import spock.lang.Specification
import spock.lang.Unroll

class VersionResolverSpec extends Specification {

    @Unroll("simple version #expression resolves correctly")
    def "simple expressions can be resolved"() {
        expect:
        VersionResolver.resolve(expression, versions) == result

        where:
        expression  | result
        '1.0.0'     | new Version('1.0.0')
        ' 1.0.0'    | new Version('1.0.0')
        '=1.0.0'    | new Version('1.0.0')
        'v1.0.0'    | new Version('1.0.0')
        '=v1.0.0'   | new Version('1.0.0')

        '<1.0.0'    | null
        '<1.1.3'    | new Version('1.0.0')
        '< 2.0.0'   | new Version('1.2.2')
        '< v1.2.2'  | new Version('1.2.1')
        '< 1.2.0'   | new Version('1.1.3')

        '>1.0.0'    | new Version('2.0.0')
        '>1.1.3'    | new Version('2.0.0')
        '> 2.0.0'   | null
        '> v1.2.2'  | new Version('2.0.0')
        '> 1.2.0'   | new Version('2.0.0')

        '<=0.0.0'   | null
        '<=1.0.0'   | new Version('1.0.0')
        '<=1.1.3'   | new Version('1.1.3')
        '<= 1.1.4'  | new Version('1.1.3')
        '<= v1.2.2' | new Version('1.2.2')
        '<= 2.0.0'  | new Version('2.0.0')

        '>=0.0.0'   | new Version('2.0.0')
        '>=1.0.0'   | new Version('2.0.0')
        '>=1.1.3'   | new Version('2.0.0')
        '>= 1.1.4'  | new Version('2.0.0')
        '>= v1.2.2' | new Version('2.0.0')
        '>= 2.0.1'  | null

        versions = versionList(['1.0.0', '1.1.3', '1.2.0', '1.2.1', '1.2.2', '2.0.0'])
    }


    List<Version> versionList(List<String> versions) {
        versions.collect { new Version(it) }
    }

}
