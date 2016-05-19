package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.Dependency
import spock.lang.Specification
import spock.lang.Subject

class DependencyBuilderSpec extends Specification {

    @Subject DependencyBuilder builder

    def setup() {
        builder = new DependencyBuilder()
    }

    def "builder can build a simple dependency"() {
        setup:
        dependencies.delegate = builder

        when:
        dependencies.call()

        then:
        formatDependencies(builder.rootDependencies) == ['foo@1.0.0']

        where:
        dependencies = {
            foo('1.0.0')
        }
    }

    def "builder can build a multiple simple dependencies"() {
        setup:
        dependencies.delegate = builder

        when:
        dependencies.call()

        then:
        formatDependencies(builder.rootDependencies) == ['bar@1.2.0', 'baz@2.0.0', 'foo@1.0.0']

        where:
        dependencies = {
            foo('1.0.0')
            bar('1.2.0')
            baz('2.0.0')
        }
    }

    def "builder can add a dependency with a git url"() {
        setup:
        dependencies.delegate = builder

        when:
        dependencies.call()
        Dependency dependency = builder.rootDependencies.first()

        then:
        dependency.name == 'foo'

        and:
        dependency.versionExpression == '1.0.0'

        and:
        dependency.fullUrl == 'http://www.example.com/foo.git'

        where:
        dependencies = {
            foo('1.0.0', url: 'http://www.example.com/foo.git')
        }
    }

    List<String> formatDependencies(List<Dependency> dependencies) {
        dependencies.collect { "${it.name}@${it.versionExpression}" }.sort()
    }

}
