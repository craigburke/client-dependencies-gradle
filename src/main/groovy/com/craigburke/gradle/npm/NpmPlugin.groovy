package com.craigburke.gradle.npm

import org.gradle.api.Plugin
import org.gradle.api.Project

class NpmPlugin implements Plugin<Project> {

    NpmExtension config
    Registry registry

    void apply(Project project) {
        config = project.extensions.create('npm', NpmExtension)

        project.task('npmClean', group: 'npm') {
            doLast {
                project.delete config.installDir, config.cacheDir
            }
        }

        project.task('npmInstall', group: 'npm') {
            mustRunAfter 'npmClean'
            doLast {
                registry.installDependencies(config.rootDependencies)
            }
        }

        project.task('npmRefresh', group: 'npm', dependsOn: ['npmClean', 'npmInstall'])

        project.afterEvaluate {
            setDefaultInstallDir(project)
            registry = new NpmRegistry(project: project, repositoryUrl: config.repositoryUrl, cacheDir: config.cacheDir, installDir: config.installDir)
        }

    }

    private void setDefaultInstallDir(Project project) {
        if (config.installDir == null) {
            boolean grailsPluginApplied = project.extensions.findByName('grails')
            config.installDir = grailsPluginApplied ? 'grails-app/assets/bower' : 'src/assets/bower'
        }
    }


}
