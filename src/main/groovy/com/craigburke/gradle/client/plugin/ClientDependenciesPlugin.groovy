package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.RootDependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.registry.BowerRegistry
import com.craigburke.gradle.client.registry.NpmRegistry
import com.craigburke.gradle.client.registry.Registry
import jsr166y.ForkJoinPool
import org.gradle.api.Plugin
import org.gradle.api.Project

import static groovyx.gpars.GParsPool.withExistingPool

class ClientDependenciesPlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'Client Dependencies'
    static final String INSTALL_TASK = 'clientInstall'
    static final String CLEAN_TASK = 'clientClean'
    static final String REFRESH_TASK = 'clientRefresh'

    ClientDependenciesExtension config
    ForkJoinPool pool = new ForkJoinPool(10)

    void apply(Project project) {
        config = project.extensions.create('clientDependencies', ClientDependenciesExtension)
        config.registryMap = [npm: new NpmRegistry(), bower: new BowerRegistry()]

        project.task(CLEAN_TASK, group: TASK_GROUP) {
            doLast {
                project.delete config.installDir
                project.delete config.cacheDir
            }
        }

        project.task(INSTALL_TASK, group: TASK_GROUP) {
            mustRunAfter CLEAN_TASK
            doLast {
                installDependencies(config.rootDependencies, project)
            }
        }

        project.task(REFRESH_TASK, group: TASK_GROUP, dependsOn: [CLEAN_TASK, INSTALL_TASK])

        project.afterEvaluate {
            setDefaults(project)
            config.registryMap.each { String key, Registry registry ->
                registry.project = project
                registry.cacheDir = project.file(config.cacheDir)
                registry.installDir = project.file(config.installDir)
            }
        }

    }

    private void installDependencies(List<RootDependency> rootDependencies, Project project) {
        withExistingPool(pool) {
            List<Dependency> loadedDependencies = rootDependencies
                    .collectParallel { RootDependency dependency ->
                        project.logger.info "Loading: ${dependency.name}@${dependency.versionExpression}"
                        dependency.registry.loadDependency(dependency as SimpleDependency)
                    }

            flattenDependencies(loadedDependencies).eachParallel { Dependency dependency ->
                Map sources = rootDependencies.find { it.name == dependency.name }?.sources ?: ['**': '']
                project.logger.info "Installing: ${dependency.name}@${dependency.version?.fullVersion}"
                dependency.registry.installDependency(dependency, sources)
            }
        }
    }

    List<Dependency> flattenDependencies(List<Dependency> dependencies) {
        dependencies + dependencies.findAll { it.children }
                .collect { flattenDependencies(it.children) }
                .unique(false) { it.name }
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
