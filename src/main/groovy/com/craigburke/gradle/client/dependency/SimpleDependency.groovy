package com.craigburke.gradle.client.dependency

import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * Base dependency class
 */
@CompileStatic
class SimpleDependency {

    private static final Pattern VERSION_EXPRESSION_PATTERN = ~/^(.*?)[@|#](.*?)$/

    String name
    String versionExpression
    String url

    void setVersionExpression(String versionExpression) {
        if (versionExpression.matches(VERSION_EXPRESSION_PATTERN)) {
            versionExpression.find(VERSION_EXPRESSION_PATTERN) { String match, String url, String tag ->
                boolean tagIsVersionExpression = tag?.matches(Version.PATTERN)
                this.url = tagIsVersionExpression ? url : match
                this.versionExpression = tagIsVersionExpression ? tag : ''
            }
        }
        else if (versionExpression.contains('/')) {
            this.url = versionExpression
            this.versionExpression = ''
        }
        else {
            this.versionExpression = versionExpression
        }

    }
}
