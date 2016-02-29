package com.craigburke.gradle.client.dependency

import spock.lang.Specification

class DependencySpec extends Specification {

    def "ancestors can be determined"() {
        given:
        Dependency dependency1 = new Dependency(name: 'dependency1')
        Dependency dependency2 = new Dependency(name: 'dependency2', parent: dependency1)
        Dependency dependency3 = new Dependency(name: 'dependency3', parent: dependency2)

        expect:
        dependency1.ancestorsAndSelf == [dependency1]

        and:
        dependency2.ancestorsAndSelf.sort { it.name } == [dependency1, dependency2]

        and:
        dependency3.ancestorsAndSelf.sort { it.name } == [dependency1, dependency2, dependency3]
    }

}
