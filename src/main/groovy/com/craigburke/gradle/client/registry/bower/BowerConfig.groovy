package com.craigburke.gradle.client.registry.bower

import com.craigburke.gradle.client.dependency.Version
import groovy.json.JsonSlurper

/**
 *
 * Utility class to deal with bower config files
 *
 * @author Craig Burke
 */
class BowerConfig {
    static final String[] CONFIG_FILES = ['bower.json', '.bower.json']

    static File getConfigFile(File sourceDir) {
        CONFIG_FILES
                .collect { String filename -> new File("${sourceDir.absolutePath}/${filename}") }
                .find { it.exists() }
    }

    static Version getVersion(File sourceDir) {
        File configFile = getConfigFile(sourceDir)
        if (configFile) {
            Map config = new JsonSlurper().parse(configFile) as Map
            Version.parse(config.version as String)
        }
        else {
            null
        }
    }
}
