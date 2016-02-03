package com.craigburke.gradle.client.dependency

import java.util.regex.Pattern

class VersionResolver {

    private static final Pattern EQUALS = ~/(?:\s*)?=?v?(\d*\.\d*\.\d*.*)?/
    private static final Pattern LESS_THAN = ~/(?:\s*)(?:<)(?:\s*)?v?(\d*\.\d*\.\d*.*)?/
    private static final Pattern GREATER_THAN = ~/(?:\s*)(?:>)(?:\s*)?v?(\d*\.\d*\.\d*.*)?/
    private static final Pattern LESS_THAN_EQUAL = ~/(?:\s*)(?:<=)(?:\s*)?v?(\d*\.\d*\.\d*.*)?/
    private static final Pattern GREATER_THAN_EQUAL = ~/(?:\s*)(?:>=)(?:\s*)?v?(\d*\.\d*\.\d*.*)?/

    static Version resolve(String expression, List<Version> versions) {
        List<Version> sortedVersions = versions.toSorted { v1, v2 -> v2 <=> v1 }
        sortedVersions.find { matches(it, expression) }
    }

    static boolean matches(Version version, String expression) {
        boolean result = false

        if (expression ==~ EQUALS) {
            expression.find(EQUALS) { String match, String versionExpression ->
                result = version == new Version(versionExpression)
            }
        }
        else if (expression ==~ LESS_THAN) {
            expression.find(LESS_THAN) { String match, String versionExpression ->
                result = version < new Version(versionExpression)
            }
        }
        else if (expression ==~ GREATER_THAN) {
            expression.find(GREATER_THAN) { String match, String versionExpression ->
                result = version > new Version(versionExpression)
            }
        }
        else if (expression ==~ LESS_THAN_EQUAL) {
            expression.find(LESS_THAN_EQUAL) { String match, String versionExpression ->
                result = version <= new Version(versionExpression)
            }
        }
        else if (expression ==~ GREATER_THAN_EQUAL) {
            expression.find(GREATER_THAN_EQUAL) { String match, String versionExpression ->
                result = version >= new Version(versionExpression)
            }
        }
        result
    }

}
