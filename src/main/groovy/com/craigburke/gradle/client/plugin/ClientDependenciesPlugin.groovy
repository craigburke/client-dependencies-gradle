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

import org.gradle.internal.os.OperatingSystem
import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.VersionResolver
import com.craigburke.gradle.client.registry.bower.BowerRegistry
import com.craigburke.gradle.client.registry.npm.NpmRegistry
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.core.AbstractRegistry
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

    static final String[] INSTALL_DEPENDENT_TASKS = ['run', 'bootRun', 'assetCompile',
        'integrationTest', 'karmaRun', 'karmaWatch']

    ClientDependenciesExtension config

    void apply(Project project) {
        config = project.extensions.create('clientDependencies', ClientDependenciesExtension, project)

        config.registries = [
                new NpmRegistry('npm'),
                new BowerRegistry('bower')
        ]

        project.task(CLEAN_TASK, group: TASK_GROUP,
                description: 'Clears the project cache and removes all files from the installDir') {
            doLast {
                project.delete config.installDir
                project.delete config.cacheDir
            }
        }

        project.task(INSTALL_TASK, group: TASK_GROUP, description: 'Resolves and installs client dependencies') {
            mustRunAfter CLEAN_TASK
            outputs.upToDateWhen {
                isClientInstallUpToDate(project)
            }
            doLast {
                installDependencies(config.rootDependencies, project)
            }
        }

        project.task(REPORT_TASK, group: TASK_GROUP, description: 'Displays a dependency tree for the client dependencies') {
            doLast {
                List<Dependency> allDependencies = loadDependencies(config.rootDependencies)
                List<Dependency> finalDependencies = Dependency.flattenList(allDependencies).unique(false) { it.name }

                project.logger.quiet ''
                printDependencies(allDependencies, finalDependencies, 1, project.logger)
            }
        }

        project.task(REFRESH_TASK, group: TASK_GROUP, dependsOn: [CLEAN_TASK, INSTALL_TASK],
                description: 'Clears project cache and re-installs all client dependencies')

        project.afterEvaluate {
            AbstractRegistry.threadPoolSize = config.threadPoolSize
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

            log.quiet "${output}"
            if (dependency.children) {
                printDependencies(dependency.children, finalDependencies, level + 1, log)
            }
        }
    }

    void installDependencies(List<Dependency> rootDependencies, Project project) {
        List<Dependency> allDependencies = loadDependencies(rootDependencies)
        List<Dependency> allDependenciesFlattened = Dependency.flattenList(allDependencies)
        List<Dependency> finalDependencies = Dependency.flattenList(allDependencies).unique(false) { it.name }

        finalDependencies.each { Dependency dependency ->
            List<Dependency> conflicts = allDependenciesFlattened
                    .findAll { it.name == dependency.name && it.version != dependency.version }
                    .findAll { !it.version.compatibleWith(dependency.version) }
                    .findAll { !VersionResolver.matches(dependency.version, it.versionExpression) }

            if (conflicts) {
                project.logger.quiet """
                    |Version conflicts found with ${dependency} [${conflicts*.versionExpression.join(', ')}]
                """.stripMargin()
            }
        }

        withExistingPool(AbstractRegistry.pool) {
            finalDependencies.eachParallel { Dependency dependency ->
                project.logger.info "Installing: ${dependency.name}@${dependency.version?.fullVersion}"

                Registry registry = dependency.registry
                Dependency rootDependency = rootDependencies.find { it.name == dependency.name }
                Closure copyConfig = rootDependency?.copyConfig ?: config.copyConfig
                String releaseFolder = dependency.getReleaseFolder(config.releaseFolders)

                project.copy {
                    into "${registry.installDir.absolutePath}/${dependency.destinationPath}"
                    from( "${dependency.sourceDir.absolutePath}/${releaseFolder}" ) {
                        with copyConfig
                    }
                }
            }
        }
    }

    List<Dependency> loadDependencies(List<Dependency> rootDependencies) {
       withExistingPool(AbstractRegistry.pool) {
            rootDependencies
                    .collectParallel { Dependency dependency ->
                List<String> siblings = rootDependencies.findAll { it.name != dependency.name }*.name
                dependency.exclude += siblings
                dependency.registry.loadDependency(dependency as Dependency, null)
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

        config.registries.each { Registry registry ->
            registry.offline = project.gradle.startParameter.isOffline()
            registry.localCacheDir = project.file("${config.cacheDir.absolutePath}/${registry.name}/")
            registry.globalCacheDir = registry.globalCacheDir ?: getDefaultGlobalCache(registry)
            registry.installDir = config.installDir
            registry.useGlobalCache = config.useGlobalCache
            registry.checkDownloads = config.checkDownloads
        }
    }

    File getDefaultGlobalCache(Registry registry) {
        OperatingSystem os = OperatingSystem.current()
        String userHome = System.getProperty('user.home') as String

        String cachePath
        if (registry instanceof NpmRegistry) {
            cachePath = os.windows ? "${userHome}/AppData/npm-cache/" : "${userHome}/.npm/"
        }
        else {
            cachePath = os.windows ? "${userHome}/AppData/Roaming/bower/cache/" : "${userHome}/.cache/bower/"
        }
        new File(cachePath)
    }

    boolean isClientInstallUpToDate(Project project) {
        config.rootDependencies.every { Dependency dependency ->
            String destinationPath = "${dependency.registry.installDir.absolutePath}/${dependency.destinationPath}"
            File destination = project.file(destinationPath)
            destination.exists() && destination.listFiles()
        }
    }

    static void setTaskDependencies(Project project) {
        INSTALL_DEPENDENT_TASKS.each { String taskName ->
            Task task = project.tasks.findByName(taskName)
            if (task) {
                task.dependsOn INSTALL_TASK
            }
        }
    }

}
