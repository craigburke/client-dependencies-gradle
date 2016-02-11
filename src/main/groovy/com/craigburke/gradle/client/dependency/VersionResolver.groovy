package com.craigburke.gradle.client.dependency

import java.util.regex.Pattern

class VersionResolver {

    private static final String VERSION_GROUP = /v?(\d*\.\d*\.\d*[^\s]*)/
    private static final String SPACES = /(?:\s*)/

    private static final Pattern EQUALS = ~/=?$SPACES$VERSION_GROUP/
    private static final Pattern LESS_THAN = ~/(?:<)$SPACES$VERSION_GROUP/
    private static final Pattern GREATER_THAN = ~/(?:>)$SPACES$VERSION_GROUP/
    private static final Pattern LESS_THAN_EQUAL = ~/(?:<=)$SPACES$VERSION_GROUP/
    private static final Pattern GREATER_THAN_EQUAL = ~/(?:>=)$SPACES$VERSION_GROUP/
    private static final Pattern CARET_RANGE = ~/(?:\^)$SPACES$VERSION_GROUP/
    private static final Pattern HYPHEN_RANGE = ~/$VERSION_GROUP$SPACES\-$SPACES$VERSION_GROUP/

    static Version resolve(String expression, List<Version> versions) {
        List<Version> sortedVersions = versions.toSorted { v1, v2 -> v2 <=> v1 }
        sortedVersions.find { matches(it, expression) }
    }

    static boolean matches(Version version, String expression) {
        expression.tokenize('||')*.trim().any { String simpleExpression ->
            matchesSimpleExpression(version, simpleExpression)
        }
    }

    private static boolean matchesSimpleExpression(Version version, String expression) {
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
        else if (expression ==~ HYPHEN_RANGE) {
            expression.find(HYPHEN_RANGE) { String match, String expression1, String expression2 ->
                Version rangeBottom = new Version(expression1)
                Version rangeTop = new Version(expression2)

                result = (version >= rangeBottom && version <= rangeTop)
            }
        }
        else if (expression ==~ CARET_RANGE) {
            expression.find(CARET_RANGE) { String match, String versionExpression ->
                result = matchesCaretRange(version, versionExpression)
            }
        }
        result
    }

    private static boolean matchesCaretRange(Version version, String rangeExpression) {
        Version rangeVersion = new Version(rangeExpression)

        if (rangeVersion.major != 0) {
            Version ceiling = new Version("${rangeVersion.major + 1}.0.0")
            return (version >= rangeVersion && version < ceiling)
        }
        else if (rangeVersion.minor != 0) {
            Version ceiling = new Version("${rangeVersion.major}.${rangeVersion.minor + 1}.0")
            return (version >= rangeVersion && version < ceiling)
        }
        else {
            return (version == rangeVersion)
        }
    }


}
