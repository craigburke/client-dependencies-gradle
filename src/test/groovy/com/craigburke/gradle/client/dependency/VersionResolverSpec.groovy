package com.craigburke.gradle.client.dependency

import spock.lang.Specification
import spock.lang.Unroll

class VersionResolverSpec extends Specification {

    @Unroll("expression #expression resolves correctly")
    def "simple expressions can be resolved"() {
        given:
        Version version = result ? new Version(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression  | result
        '1.0.0'     | '1.0.0'
        ' 1.0.0'    | '1.0.0'
        '=1.0.0'    | '1.0.0'
        'v1.0.0'    | '1.0.0'
        '=v1.0.0'   | '1.0.0'

        '<1.0.0'    | null
        '<1.1.3'    | '1.0.0'
        '< 2.0.0'   | '1.2.2'
        '< v1.2.2'  | '1.2.1'
        '< 1.2.0'   | '1.1.3'

        '>1.0.0'    | '2.0.0'
        '>1.1.3'    | '2.0.0'
        '> 2.0.0'   | null
        '> v1.2.2'  | '2.0.0'
        '> 1.2.0'   | '2.0.0'

        '<=0.0.0'   | null
        '<=1.0.0'   | '1.0.0'
        '<=1.1.3'   | '1.1.3'
        '<= 1.1.4'  | '1.1.3'
        '<= v1.2.2' | '1.2.2'
        '<= 2.0.0'  | '2.0.0'

        '>=0.0.0'   | '2.0.0'
        '>=1.0.0'   | '2.0.0'
        '>=1.1.3'   | '2.0.0'
        '>= 1.1.4'  | '2.0.0'
        '>= v1.2.2' | '2.0.0'
        '>= 2.0.1'  | null

        versions = ['1.0.0', '1.1.3', '1.2.0', '1.2.1', '1.2.2', '2.0.0']
    }

    @Unroll("#expression resolves correctly")
    def "caret range expressions can be resolved"() {
        given:
        Version version = result ? new Version(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression  | result
        '^0.0.1'    | '0.0.1'
        '^0.0.4'    | null

        '^0.1.0'    | '0.1.4'
        '^0.1.4'    | '0.1.4'
        '^0.1.5'    | null

        '^1.0.0'    | '1.3.1'
        '^1.3.1'    | '1.3.1'
        '^1.3.2'    | null

        versions = ['0.0.1', '0.0.2', '0.0.3', '0.0.5', '0.0.6',
                    '0.1.0', '0.1.1', '0.1.2', '0.1.3', '0.1.4',
                    '1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.3.1',
                    '2.0.0']
    }
}
