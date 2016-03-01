/*
 * Copyright 2016 Craig Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craigburke.gradle.client.plugin

import static groovyx.gpars.GParsPool.withExistingPool

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.registry.BowerRegistry
import com.craigburke.gradle.client.registry.NpmRegistry
import com.craigburke.gradle.client.registry.Registry
import com.craigburke.gradle.client.registry.RegistryBase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

/**
 *
 * Main client dependencies plugin class
 *
 * @author Craig Burke
 */
class ClientDependenciesPlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'Client Dependencies'
    static final String INSTALL_TASK = 'clientInstall'
    static final String CLEAN_TASK = 'clientClean'
    static final String REFRESH_TASK = 'clientRefresh'
    static final String REPORT_TASK = 'clientReport'

    static final String[] INSTALL_DEPENDENT_TASKS = ['run', 'bootRun', 'assetCompile', 'karmaRun', 'karmaWatch']
    static final String[] CLEAN_DEPENDENT_TASKS = ['clean']

    ClientDependenciesExtension config

    void apply(Project project) {
        config = project.extensions.create('clientDependencies', ClientDependenciesExtension, project)
        config.registryMap = [npm: new NpmRegistry(), bower: new BowerRegistry()]

        project.task(CLEAN_TASK, group: TASK_GROUP) {
            doLast {
                project.delete config.installDir
                project.delete config.cacheDir
            }
        }

        project.task(INSTALL_TASK, group: TASK_GROUP) {
            mustRunAfter CLEAN_TASK
            outputs.upToDateWhen {
                project.file(config.installDir).exists()
            }
            doLast {
                installDependencies(config.rootDependencies, project)
            }
        }

        project.task(REPORT_TASK, group: TASK_GROUP) {
            doLast {
                List<Dependency> allDependencies = loadDependencies(config.rootDependencies, project)
                List<Dependency> finalDependencies = Dependency.flattenList(allDependencies).unique(false) { it.name }

                project.logger.quiet '\n'
                printDependencies(allDependencies, finalDependencies, 1, project.logger)
            }
        }

        project.task(REFRESH_TASK, group: TASK_GROUP, dependsOn: [CLEAN_TASK, INSTALL_TASK])

        project.afterEvaluate {
            RegistryBase.threadPoolSize = config.threadPoolSize
            setDefaults(project)
            setTaskDependencies(project)
        }

    }

    void printDependencies(List<Dependency> dependencies, List<Dependency> finalDependencies, int level, Logger log) {
        dependencies.each { Dependency dependency ->
            String output = ('|    ' * (level - 1)) + '+--- '
            output += "${dependency.name}@"

            if (dependency.versionExpression.contains(' ')) {
                output += "(${dependency.versionExpression})"
            }
            else {
                output += dependency.versionExpression
            }

            Dependency installedDependency = finalDependencies.find { it.name == dependency.name }

            if (dependency.version != installedDependency.version) {
                output += " -> ${installedDependency.version.fullVersion} (*)"
            }
            else if (dependency.version.fullVersion != dependency.versionExpression) {
                output += " -> ${dependency.version.fullVersion}"
            }

            log.quiet "${output}\n"
            if (dependency.children) {
                printDependencies(dependency.children, finalDependencies, level + 1, log)
            }
        }
    }

    void installDependencies(List<DeclaredDependency> rootDependencies, Project project) {
        List<Dependency> allDependencies = loadDependencies(rootDependencies, project)
        List<Dependency> allDependenciesFlattened = Dependency.flattenList(allDependencies)
        List<Dependency> finalDependencies = Dependency.flattenList(allDependencies).unique(false) { it.name }

        finalDependencies.each { Dependency dependency ->
            List<Dependency> conflicts = allDependenciesFlattened
                    .findAll { it.name == dependency.name && !it.version.compatibleWith(dependency.version) }

            if (conflicts) {
                project.logger.quiet """\
                    Version conflict with module ${dependency.name}.
                    Declared versions: ${dependency.versionExpression}, ${conflicts*.versionExpression.join(', ')}
                """
            }
        }

        withExistingPool(RegistryBase.pool) {
            finalDependencies.eachParallel { Dependency dependency ->
                DeclaredDependency rootDependency = rootDependencies.find { it.name == dependency.name }
                Registry registry = dependency.registry

                project.logger.info "Installing: ${dependency.name}@${dependency.version?.fullVersion}"
                File sourceFolder = registry.getSourceFolder(dependency)
                Closure copyConfig = rootDependency?.copyConfig ?: config.getDefaultCopyConfig(sourceFolder)

                project.copy {
                    includeEmptyDirs = false
                    into "${registry.installPath}/${dependency.name}"
                    from("${sourceFolder}") {
                        with copyConfig
                    }
                }
            }
        }
    }

    List<Dependency> loadDependencies(List<DeclaredDependency> rootDependencies, Project project) {
       withExistingPool(RegistryBase.pool) {
            rootDependencies
                    .collectParallel { DeclaredDependency dependency ->
                project.logger.info "Loading: ${dependency.name}@${dependency.versionExpression}"
                dependency.registry.loadDependency(dependency as DeclaredDependency, null)
            }
        } as List<Dependency>
    }

    void setDefaults(Project project) {
        if (config.installDir == null) {
            boolean usesGrails = project.extensions.findByName('grails')
            config.installDir = project.file(usesGrails ? 'grails-app/assets/vendor' : 'src/assets/vendor').absolutePath
        }

        if (config.cacheDir == null) {
            config.cacheDir = "${project.buildDir.path}/client-cache"
        }

        config.registryMap.each { String key, Registry registry ->
            registry.cachePath = project.file(config.cacheDir).absolutePath
            registry.installPath = project.file(config.installDir).absolutePath
        }
    }

    static void setTaskDependencies(Project project) {
        INSTALL_DEPENDENT_TASKS.each { String taskName ->
            Task task = project.tasks.findByName(taskName)
            if (task) {
                task.dependsOn INSTALL_TASK
            }
        }
        CLEAN_DEPENDENT_TASKS.each { String taskName ->
            Task task = project.tasks.findByName(taskName)
            if (task) {
                task.dependsOn CLEAN_TASK
            }
        }
    }

}
