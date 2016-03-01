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

}

