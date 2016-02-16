package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.registry.BowerRegistry
import com.craigburke.gradle.client.registry.NpmRegistry
import com.craigburke.gradle.client.registry.Registry
import com.craigburke.gradle.client.registry.RegistryBase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails

import static groovyx.gpars.GParsPool.withExistingPool

class ClientDependenciesPlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'Client Dependencies'
    static final String INSTALL_TASK = 'clientInstall'
    static final String CLEAN_TASK = 'clientClean'
    static final String REFRESH_TASK = 'clientRefresh'
    static final String[] DEPENDENT_TASKS = ['run', 'bootRun', 'assetCompile', 'karmaRun', 'karmaWatch']

    ClientDependenciesExtension config

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
            setTaskDependencies(project)
        }

    }

    void installDependencies(List<DeclaredDependency> rootDependencies, Project project) {
        withExistingPool(RegistryBase.pool) {
            List<Dependency> loadedDependencies = rootDependencies
                    .collectParallel { DeclaredDependency dependency ->
                        project.logger.info "Loading: ${dependency.name}@${dependency.versionExpression}"
                        dependency.registry.loadDependency(dependency as DeclaredDependency)
                    }


            Dependency.flattenList(loadedDependencies).eachParallel { Dependency dependency ->
                DeclaredDependency rootDependency = rootDependencies.find { it.name == dependency.name }
                Registry registry = dependency.registry

                project.logger.info "Installing: ${dependency.name}@${dependency.version?.fullVersion}"
                Closure copyConfig = rootDependency?.copyConfig ?: getDefaultCopyConfig(dependency)

                project.copy {
                    includeEmptyDirs = false
                    into "${registry.installPath}/${dependency.name}"
                    from("${registry.getSourceFolder(dependency)}") {
                        with copyConfig
                    }
                }
            }
        }
    }
    void setDefaults(Project project) {
        if (config.installDir == null) {
            boolean grailsPluginApplied = project.extensions.findByName('grails')
            config.installDir = project.file(grailsPluginApplied ? 'grails-app/assets/vendor' : 'src/assets/vendor').absolutePath
        }

        if (config.cacheDir == null) {
            config.cacheDir = "${project.buildDir.path}/client-cache"
        }

        config.registryMap.each { String key, Registry registry ->
            registry.cachePath = project.file(config.cacheDir).absolutePath
            registry.installPath = project.file(config.installDir).absolutePath
        }
    }

    Closure getDefaultCopyConfig(Dependency dependency) {
        Registry registry = dependency.registry
        File sourceFolder = registry.getSourceFolder(dependency)

        return {
            exclude '**/*.min.js', '**/*.min.css', '**/Gruntfile.js'

            if (sourceFolder.listFiles().find { it.directory && it.name == 'dist'}) {
                include 'dist/**/*.js', 'dist/**/*.css'
                eachFile { FileCopyDetails fileCopyDetails ->
                    fileCopyDetails.path -= 'dist/'
                }
            }
            else {
                include '**/*.js', '**/*.css'
            }
        }
    }

    static void setTaskDependencies(Project project) {
        DEPENDENT_TASKS.each { String taskName ->
            def task = project.tasks.findByName(taskName)
            if (task) {
                task.dependsOn INSTALL_TASK
            }
        }
    }

}
