package com.craigburke.gradle.npm

import org.gradle.api.Plugin
import org.gradle.api.Project

class ClientDependenciesPlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'Client Dependencies'
    static final String INSTALL_TASK = 'clientInstall'
    static final String CLEAN_TASK = 'clientClean'
    static final String REFRESH_TASK = 'clientRefresh'

    ClientDependenciesExtension config

    void apply(Project project) {
        config = project.extensions.create('clientDependencies', ClientDependenciesExtension)

        project.task(CLEAN_TASK, group: TASK_GROUP) {
            doLast {
                project.delete config.registry.installDir
                project.delete config.registry.cacheDir
            }
        }

        project.task(INSTALL_TASK, group: TASK_GROUP) {
            mustRunAfter CLEAN_TASK
            doLast {
                config.registry.installDependencies(config.rootDependencies)
            }
        }

        project.task(REFRESH_TASK, group: TASK_GROUP, dependsOn: [CLEAN_TASK, INSTALL_TASK])

        project.afterEvaluate {
            setDefaults(project)
            config.registry = new NpmRegistry(installDir: config.installDir, cacheDir: config.cacheDir, project: project)
        }

    }

    private void setDefaults(Project project) {

        if (config.installDir == null) {
            boolean grailsPluginApplied = project.extensions.findByName('grails')
            config.installDir = grailsPluginApplied ? 'grails-app/assets/vendor' : 'src/assets/vendor'
        }

        if (config.cacheDir == null) {
            config.cacheDir = "${project.buildDir.path}/client-cache"
        }

    }


}
