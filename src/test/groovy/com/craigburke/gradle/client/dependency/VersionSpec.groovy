package com.craigburke.gradle.client.dependency

import spock.lang.Specification
import spock.lang.Unroll

class VersionSpec extends Specification {

    @Unroll('Simple version #versionExpression can be created')
    def "Can create simple versions"() {
        when:
        Version version = new Version(versionExpression)

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
        Version version = new Version(versionExpression)

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
        Version version = new Version(versionExpression)

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


}
