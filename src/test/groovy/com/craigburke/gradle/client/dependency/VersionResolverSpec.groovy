package com.craigburke.gradle.client.dependency

import spock.lang.Specification
import spock.lang.Unroll

class VersionResolverSpec extends Specification {

    @Unroll
    def "Simple expression #expression resolves correctly"() {
        given:
        Version version = result ? Version.parse(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression      | result
        '1.0.0'         | '1.0.0'
        ' 1.0.0'        | '1.0.0'
        '=1.0.0'        | '1.0.0'
        'v1.0.0'        | '1.0.0'
        '=v1.0.0'       | '1.0.0'

        '<1.0.0'        | null
        '<1.1.3'        | '1.0.0'
        '< 2.0.0'       | '1.2.2'
        '< v1.2.2'      | '1.2.1'
        '< 1.2.0'       | '1.1.3'

        '>1.0.0'        | '2.0.0'
        '>1.1.3'        | '2.0.0'
        '> 2.0.0'       | null
        '> v1.2.2'      | '2.0.0'
        '> 1.2.0'       | '2.0.0'

        '<=0.0.0'       | null
        '<=1.0.0'       | '1.0.0'
        '<=1.1.3'       | '1.1.3'
        '<= 1.1.4'      | '1.1.3'
        '<= v1.2.2'     | '1.2.2'
        '<= 2.0.0'      | '2.0.0'

        '>=0.0.0'       | '2.0.0'
        '>=1.0.0'       | '2.0.0'
        '>=1.1.3'       | '2.0.0'
        '>= 1.1.4'      | '2.0.0'
        '>= v1.2.2'     | '2.0.0'
        '>= 2.0.1'      | null

        '1.2.1 - 1.2.9' | '1.2.2'
        '1.2.2 - 1.2.3' | '1.2.2'
        '1.3.0 - 2.0.0' | '2.0.0'
        '1.3.0 - 2.1.0' | '2.0.0'

        versions = ['1.0.0', '1.1.3', '1.2.0', '1.2.1', '1.2.2', '2.0.0']
    }

    @Unroll
    def "caret range expressions #expression resolves correctly"() {
        given:
        Version version = result ? Version.parse(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression | result
        '^0.0.1'   | '0.0.1'
        '^0.0.4'   | null

        '^0.1.0'   | '0.1.4'
        '^0.1.4'   | '0.1.4'
        '^0.1.5'   | null

        '^1.0.0'   | '1.3.1'
        '^1.3.1'   | '1.3.1'
        '^1.3.2'   | null
        '^1.1'     | '1.3.1'
        '^1'       | '1.3.1'

        versions = ['0.0.1', '0.0.2', '0.0.3', '0.0.5', '0.0.6',
                    '0.1.0', '0.1.1', '0.1.2', '0.1.3', '0.1.4',
                    '1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.3.1',
                    '2.0.0']
    }

    @Unroll
    def "combined expressions #expression resolves correctly"() {
        given:
        Version version = result ? Version.parse(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression            | result
        '0.0.1 || 0.0.2'      | '0.0.2'
        '> 0.0.3 || < 0.0.6'  | '2.0.0'
        '< 0.0.1 || <= 1.0.0' | '1.0.0'
        '> 0.0.3 < 0.0.6'     | '0.0.5'
        '< 0.0.1 <= 1.0.0'    | null

        versions = ['0.0.1', '0.0.2', '0.0.3', '0.0.5', '0.0.6',
                    '0.1.0', '0.1.1', '0.1.2', '0.1.3', '0.1.4',
                    '1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.3.1',
                    '2.0.0']
    }

    @Unroll
    def "tagged release #expression resolves to #result"() {
        given:
        Version version = result ? Version.parse(result) : null
        List<Version> versionList = Version.toList(versions)

        expect:
        VersionResolver.resolve(expression, versionList) == version

        where:
        expression      | result
        '=1.0.0'        | null
        '=1.0.0-alpha'  | '1.0.0-alpha'
        '> 1.0.0-alpha' | '2.0.2-alpha'
        '> 1.0.0'       | '2.0.1'
        '> 1.3.1'       | '2.0.1'
        '1.x.x'         | '1.3.1'

        versions = ['1.0.0-alpha', '1.1.0-alpha', '1.2.0',
                    '1.3.0', '1.3.1', '2.0.0-alpha', '2.0.1', '2.0.2-alpha']
    }

    @Unroll
    "version #versionExpression is included in range #rangeExpression"() {
        given:
        Version version = Version.parse(versionExpression)

        expect:
        VersionResolver.matches(version, rangeExpression)

        where:
        rangeExpression                   | versionExpression
        '1.0.0 - 2.0.0'                   | '1.2.3'
        '^1.2.3+build'                    | '1.2.3'
        '^1.2.3+build'                    | '1.3.0'
        '1.2.3-pre+asdf - 2.4.3-pre+asdf' | '1.2.3'
        '1.2.3-pre+asdf - 2.4.3-pre+asdf' | '1.2.3-pre.2'
        '1.2.3-pre+asdf - 2.4.3-pre+asdf' | '2.4.3-alpha'
        '1.2.3+asdf - 2.4.3+asdf'         | '1.2.3'
        '1.0.0'                           | '1.0.0'
        '>=*'                             | '0.2.4'
        ''                                | '1.0.0'
        '*'                               | '1.2.3'
        '*'                               | 'v1.2.3'
        '>=1.0.0'                         | '1.0.0'
        '>=1.0.0'                         | '1.0.1'
        '>=1.0.0'                         | '1.1.0'
        '>1.0.0'                          | '1.0.1'
        '>1.0.0'                          | '1.1.0'
        '<=2.0.0'                         | '2.0.0'
        '<=2.0.0'                         | '1.9999.9999'
        '<=2.0.0'                         | '0.2.9'
        '<2.0.0'                          | '1.9999.9999'
        '<2.0.0'                          | '0.2.9'
        '>= 1.0.0'                        | '1.0.0'
        '>=  1.0.0'                       | '1.0.1'
        '>=   1.0.0'                      | '1.1.0'
        '> 1.0.0'                         | '1.0.1'
        '>  1.0.0'                        | '1.1.0'
        '<=   2.0.0'                      | '2.0.0'
        '<= 2.0.0'                        | '1.9999.9999'
        '<=  2.0.0'                       | '0.2.9'
        '<    2.0.0'                      | '1.9999.9999'
        '<\t2.0.0'                        | '0.2.9'
        '>=0.1.97'                        | 'v0.1.97'
        '>=0.1.97'                        | '0.1.97'
        '0.1.20 || 1.2.4'                 | '1.2.4'
        '>=0.2.3 || <0.0.1'               | '0.0.0'
        '>=0.2.3 || <0.0.1'               | '0.2.3'
        '>=0.2.3 || <0.0.1'               | '0.2.4'
        '||'                              | '1.3.4'
        '2.x.x'                           | '2.1.3'
        '1.2.x'                           | '1.2.3'
        '1.2.x || 2.x'                    | '2.1.3'
        '1.2.x || 2.x'                    | '1.2.3'
        'x'                               | '1.2.3'
        '2.*.*'                           | '2.1.3'
        '1.2.*'                           | '1.2.3'
        '1.2.* || 2.*'                    | '2.1.3'
        '1.2.* || 2.*'                    | '1.2.3'
        '*'                               | '1.2.3'
        '2'                               | '2.1.2'
        '2.3'                             | '2.3.1'
        '~2.4'                            | '2.4.0' // >=2.4.0 <2.5.0
        '~2.4'                            | '2.4.5'
        '~>3.2.1'                         | '3.2.2' // >=3.2.1 <3.3.0|
        '~1'                              | '1.2.3' // >=1.0.0 <2.0.0
        '~>1'                             | '1.2.3'
        '~> 1'                            | '1.2.3'
        '~1.0'                            | '1.0.2' // >=1.0.0 <1.1.0|
        '~ 1.0'                           | '1.0.2'
        '~ 1.0.3'                         | '1.0.12'
        '>=1'                             | '1.0.0'
        '>= 1'                            | '1.0.0'
        '<1.2'                            | '1.1.1'
        '< 1.2'                           | '1.1.1'
        '~v0.5.4-pre'                     | '0.5.5'
        '~v0.5.4-pre'                     | '0.5.4'
        '=0.7.x'                          | '0.7.2'
        '<=0.7.x'                         | '0.7.2'
        '>=0.7.x'                         | '0.7.2'
        '<=0.7.x'                         | '0.6.2'
        '~1.2.1 >=1.2.3'                  | '1.2.3'
        '~1.2.1 =1.2.3'                   | '1.2.3'
        '~1.2.1 1.2.3'                    | '1.2.3'
        '~1.2.1 >=1.2.3 1.2.3'            | '1.2.3'
        '~1.2.1 1.2.3 >=1.2.3'            | '1.2.3'
        '~1.2.1 1.2.3'                    | '1.2.3'
        '>=1.2.1 1.2.3'                   | '1.2.3'
        '1.2.3 >=1.2.1'                   | '1.2.3'
        '>=1.2.3 >=1.2.1'                 | '1.2.3'
        '>=1.2.1 >=1.2.3'                 | '1.2.3'
        '>=1.2'                           | '1.2.8'
        '^1.2.3'                          | '1.8.1'
        '^0.1.2'                          | '0.1.2'
        '^0.1'                            | '0.1.2'
        '^1.2'                            | '1.4.2'
        '^1.2 ^1'                         | '1.4.2'
        '^1.2.3-alpha'                    | '1.2.3-pre'
        '^1.2.0-alpha'                    | '1.2.0-pre'
        '^0.0.1-alpha'                    | '0.0.1-beta'
    }

    @Unroll
    "version #versionExpression is not included in range #rangeExpression"() {
        given:
        Version version = Version.parse(versionExpression)

        expect:
        !VersionResolver.matches(version, rangeExpression)

        where:
        rangeExpression                                | versionExpression
        '1.0.0 - 2.0.0'                                | '2.2.3'
        '1.2.3+asdf - 2.4.3+asdf'                      | '1.2.3-pre.2'
        '1.2.3+asdf - 2.4.3+asdf'                      | '2.4.3-alpha'
        '^1.2.3+build'                                 | '2.0.0'
        '^1.2.3+build'                                 | '1.2.0'
        '^1.2.3'                                       | '1.2.3-pre'
        '^1.2'                                         | '1.2.0-pre'
        '>1.2'                                         | '1.3.0-beta'
        '<=1.2.3'                                      | '1.2.3-beta'
        '^1.2.3'                                       | '1.2.3-beta'
        '=0.7.x'                                       | '0.7.0-asdf'
        '>=0.7.x'                                      | '0.7.0-asdf'
        '1.0.0'                                        | '1.0.1'
        '>=1.0.0'                                      | '0.0.0'
        '>=1.0.0'                                      | '0.0.1'
        '>=1.0.0'                                      | '0.1.0'
        '>1.0.0'                                       | '0.0.1'
        '>1.0.0'                                       | '0.1.0'
        '<=2.0.0'                                      | '3.0.0'
        '<=2.0.0'                                      | '2.9999.9999'
        '<=2.0.0'                                      | '2.2.9'
        '<2.0.0'                                       | '2.9999.9999'
        '<2.0.0'                                       | '2.2.9'
        '>=0.1.97'                                     | 'v0.1.93'
        '>=0.1.97'                                     | '0.1.93'
        '0.1.20  1.2.4'                                | '1.2.3'
        '>=0.2.3  <0.0.1'                              | '0.0.3'
        '>=0.2.3  <0.0.1'                              | '0.2.2'
        '2.x.x'                                        | '1.1.3'
        '2.x.x'                                        | '3.1.3'
        '1.2.x'                                        | '1.3.3'
        '1.2.x  2.x'                                   | '3.1.3'
        '1.2.x  2.x'                                   | '1.1.3'
        '2.*.*'                                        | '1.1.3'
        '2.*.*'                                        | '3.1.3'
        '1.2.*'                                        | '1.3.3'
        '1.2.*  2.*'                                   | '3.1.3'
        '1.2.*  2.*'                                   | '1.1.3'
        '2'                                            | '1.1.2'
        '2.3'                                          | '2.4.1'
        '~2.4'                                         | '2.5.0' // >=2.4.0 <2.5.0
        '~2.4'                                         | '2.3.9'
        '~>3.2.1'                                      | '3.3.2' // >=3.2.1 <3.3.0
        '~>3.2.1'                                      | '3.2.0' // >=3.2.1 <3.3.0
        '~1'                                           | '0.2.3' // >=1.0.0 <2.0.0
        '~>1'                                          | '2.2.3'
        '~1.0'                                         | '1.1.0' // >=1.0.0 <1.1.0
        '<1'                                           | '1.0.0'
        '>=1.2'                                        | '1.1.1'
        '1'                                            | '2.0.0beta'
        '~v0.5.4-beta'                                 | '0.5.4-alpha'
        '=0.7.x'                                       | '0.8.2'
        '>=0.7.x'                                      | '0.6.2'
        '<0.7.x'                                       | '0.7.2'
        '<1.2.3'                                       | '1.2.3-beta'
        '=1.2.3'                                       | '1.2.3-beta'
        '>1.2'                                         | '1.2.8'
        '^1.2.3'                                       | '2.0.0-alpha'
        '^1.2.3'                                       | '1.2.2'
        '^1.2'                                         | '1.1.9'
    }
}
