package com.craigburke.gradle.client.dependency

import spock.lang.Specification
import spock.lang.Unroll

class VersionSpec extends Specification {

    @Unroll('Simple version #versionExpression can be created')
    def "Can create simple versions"() {
        when:
        Version version = Version.parse(versionExpression)

        then:
        version.major == major

        and:
        version.minor == minor

        and:
        version.patch == patch

        and:
        version.fullVersion == versionExpression

        and:
        version.simpleVersion == versionExpression

        where:
        major | minor | patch
        1     | 2     | 3
        0     | 0     | 0
        99    | 99    | 99

        versionExpression = "${major}.${minor}.${patch}" as String
    }

    @Unroll('version #versionExpression can be created')
    def "Can create versions with tags and build info"() {
        when:
        Version version = Version.parse(versionExpression)

        then:
        version.tag == tag

        and:
        version.build == build

        and:
        version.simpleVersion == simpleVersion

        and:
        version.fullVersion == versionExpression

        where:
        simpleVersion | tag      | build
        '1.0.0'       | 'beta.1' | ''
        '1.2.0'       | 'beta.2' | ''
        '0.0.0'       | 'alpha'  | ''
        '0.0.0'       | 'alpha'  | ''
        '1.0.0'       | 'beta.1' | '12354678'
        '1.2.0'       | 'beta.2' | 'L9ThxnotKP'
        '0.0.0'       | 'alpha'  | 'L9ThxnotKP'
        '0.0.0'       | 'alpha'  | '12354678'
        '1.0.0'       | ''       | '12354678'
        '1.2.0'       | ''       | 'L9ThxnotKP'
        '0.0.0'       | ''       | 'L9ThxnotKP'
        '0.0.0'       | ''       | '12354678'

        tagExpression = tag ? "-${tag}" : ''
        buildExpression = build ? "+${build}" : ''
        versionExpression = "${simpleVersion}${tagExpression}${buildExpression}" as String
    }

    @Unroll
    def "Can create fuzzy version #versionExpression"() {
        when:
        Version version = Version.parse(versionExpression)

        then:
        version.major == major

        and:
        version.minor == minor

        and:
        version.patch == patch

        and:
        version.fullVersion == fullVersion

        and:
        version.fuzzy

        where:
        versionExpression | major | minor | patch | fullVersion
        '1.1.x'           | 1     | 1     | null  | '1.1.x'
        '1.1.X'           | 1     | 1     | null  | '1.1.x'
        '1.1.*'           | 1     | 1     | null  | '1.1.x'
        '1.1'             | 1     | 1     | null  | '1.1.x'
        '1.x'             | 1     | null  | null  | '1.x.x'
        '1.X'             | 1     | null  | null  | '1.x.x'
        '1.*'             | 1     | null  | null  | '1.x.x'
        '1'               | 1     | null  | null  | '1.x.x'
        'x'               | null  | null  | null  | 'x.x.x'
        'X'               | null  | null  | null  | 'x.x.x'
        '*'               | null  | null  | null  | 'x.x.x'
        ''                | null  | null  | null  | 'x.x.x'
    }

    @Unroll
    def "floor and ceiling values are correct for #versionExpression"() {
        when:
        Version version = Version.parse(versionExpression)

        then:
        version.floor == Version.parse(floor)

        and:
        version.ceiling == Version.parse(ceiling)

        where:
        versionExpression | floor   | ceiling
        '1.2.3'           | '1.2.3' | '1.2.3'
        '1.1.x'           | '1.1.0' | '1.2.0'
        '1.1.X'           | '1.1.0' | '1.2.0'
        '1.1.*'           | '1.1.0' | '1.2.0'
        '1.1'             | '1.1.0' | '1.2.0'
        '1.x'             | '1.0.0' | '2.0.0'
        '1.X'             | '1.0.0' | '2.0.0'
        '1.*'             | '1.0.0' | '2.0.0'
        '1'               | '1.0.0' | '2.0.0'
        'x'               | '0.0.0' | '1.0.0'
        'X'               | '0.0.0' | '1.0.0'
        '*'               | '0.0.0' | '1.0.0'
        ''                | '0.0.0' | '1.0.0'
    }

    @Unroll
    def "leading v is ignored in expression #expression"() {
        expect:
        Version.parse(expression).fullVersion == result

        where:
        expression | result
        'v1.0.0'   | '1.0.0'
        '1.0.0'    | '1.0.0'
    }

    @Unroll
    def "Version #v1 #messageVerb #v2"() {
        setup:
        Version version1 = Version.parse(v1)
        Version version2 = Version.parse(v2)

        expect:
        version1.compatibleWith(version2) == result

        and:
        version2.compatibleWith(version1) == result

        where:
        v1          | v2          | result
        '1.0.0'     | '1.0.1'     | true
        '1.0.0'     | '1.1.1'     | true
        '1.0.0'     | '2.0.0'     | false
        '1.0.0-foo' | '1.0.0-foo' | true
        '1.0.0-foo' | '1.0.0-bar' | false

        messageVerb = result ? 'is compatible with' : 'is not compatible with'
    }

    @Unroll
    def "Version #result is detectable despite extra characters in '#expression'"() {
        expect:
        Version.parse(expression).toString() == result

        where:
        expression     | result
        '1.2.3'        | '1.2.3'
        ' 1.2.3 '      | '1.2.3'
        ' 1.2.3-4 '    | '1.2.3-4'
        ' 1.2.3-pre '  | '1.2.3-pre'
        '  =v1.2.3   ' | '1.2.3'
        'v1.2.3'       | '1.2.3'
        ' v1.2.3 '     | '1.2.3'
        '\t1.2.3'      | '1.2.3'
    }

    @Unroll
    def "Version #versionExpression1 should be greater than #versionExpression2"() {
        setup:
        Version version1 = Version.parse(versionExpression1)
        Version version2 = Version.parse(versionExpression2)

        expect:
        version1 > version2

        where:
        versionExpression1   | versionExpression2
        '0.0.0'              | '0.0.0-foo'
        '0.0.1'              | '0.0.0'
        '1.0.0'              | '0.9.9'
        '0.10.0'             | '0.9.0'
        '0.99.0'             | '0.10.0'
        '2.0.0'              | '1.2.3'
        'v0.0.0'             | '0.0.0-foo'
        'v0.0.1'             | '0.0.0'
        'v1.0.0'             | '0.9.9'
        'v0.10.0'            | '0.9.0'
        'v0.99.0'            | '0.10.0'
        'v2.0.0'             | '1.2.3'
        '0.0.0'              | 'v0.0.0-foo'
        '0.0.1'              | 'v0.0.0'
        '1.0.0'              | 'v0.9.9'
        '0.10.0'             | 'v0.9.0'
        '0.99.0'             | 'v0.10.0'
        '2.0.0'              | 'v1.2.3'
        '1.2.3'              | '1.2.3-asdf'
        '1.2.3'              | '1.2.3-4'
        '1.2.3'              | '1.2.3-4-foo'
        '1.2.3-5-foo'        | '1.2.3-5'
        '1.2.3-5'            | '1.2.3-4'
        '1.2.3-5-foo'        | '1.2.3-5-Foo'
        '3.0.0'              | '2.7.2+asdf'
        '1.2.3-a.10'         | '1.2.3-a.5'
        '1.2.3-a.b'          | '1.2.3-a.5'
        '1.2.3-a.b'          | '1.2.3-a'
        '1.2.3-a.b.c.10.d.5' | '1.2.3-a.b.c.5.d.100'
        '1.2.3-r2'           | '1.2.3-r100'
        '1.2.3-r100'         | '1.2.3-R2'
    }

    @Unroll
    def "Version #versionExpression1 should be equal to #versionExpression2"() {
        setup:
        Version version1 = Version.parse(versionExpression1)
        Version version2 = Version.parse(versionExpression2)

        expect:
        version1 == version2

        where:
        versionExpression1 | versionExpression2
        '1.2.3'            | 'v1.2.3'
        '1.2.3'            | '=1.2.3'
        '1.2.3'            | 'v 1.2.3'
        '1.2.3'            | '= 1.2.3'
        '1.2.3'            | ' v1.2.3'
        '1.2.3'            | ' =1.2.3'
        '1.2.3'            | ' v 1.2.3'
        '1.2.3'            | ' = 1.2.3'
        '1.2.3-0'          | 'v1.2.3-0'
        '1.2.3-0'          | '=1.2.3-0'
        '1.2.3-0'          | 'v 1.2.3-0'
        '1.2.3-0'          | '= 1.2.3-0'
        '1.2.3-0'          | ' v1.2.3-0'
        '1.2.3-0'          | ' =1.2.3-0'
        '1.2.3-0'          | ' v 1.2.3-0'
        '1.2.3-0'          | ' = 1.2.3-0'
        '1.2.3-1'          | 'v1.2.3-1'
        '1.2.3-1'          | '=1.2.3-1'
        '1.2.3-1'          | 'v 1.2.3-1'
        '1.2.3-1'          | '= 1.2.3-1'
        '1.2.3-1'          | ' v1.2.3-1'
        '1.2.3-1'          | ' =1.2.3-1'
        '1.2.3-1'          | ' v 1.2.3-1'
        '1.2.3-1'          | ' = 1.2.3-1'
        '1.2.3-beta'       | 'v1.2.3-beta'
        '1.2.3-beta'       | '=1.2.3-beta'
        '1.2.3-beta'       | 'v 1.2.3-beta'
        '1.2.3-beta'       | '= 1.2.3-beta'
        '1.2.3-beta'       | ' v1.2.3-beta'
        '1.2.3-beta'       | ' =1.2.3-beta'
        '1.2.3-beta'       | ' v 1.2.3-beta'
        '1.2.3-beta'       | ' = 1.2.3-beta'
        '1.2.3-beta+build' | ' = 1.2.3-beta+otherbuild'
        '1.2.3+build'      | ' = 1.2.3+otherbuild'
        '1.2.3-beta+build' | '1.2.3-beta+otherbuild'
        '1.2.3+build'      | '1.2.3+otherbuild'
        '  v1.2.3+build'   | '1.2.3+otherbuild'
    }

}

