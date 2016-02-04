package com.craigburke.gradle.client.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

import static ClientDependenciesPlugin.*

class ClientDependenciesPluginSpec extends Specification {

    static final String PLUGIN_NAME = 'com.craigburke.client-dependencies'

    @Unroll
    def "#taskName added to project when plugin is applied"() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        project.tasks[taskName]

        where:
        taskName << [CLEAN_TASK, INSTALL_TASK, REFRESH_TASK]
    }

}
