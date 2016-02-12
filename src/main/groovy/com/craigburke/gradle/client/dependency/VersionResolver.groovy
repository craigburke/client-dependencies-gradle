package com.craigburke.gradle.client.dependency

import java.util.regex.Pattern

class VersionResolver {

    private static final String VERSION_GROUP = /v?(${Version.PATTERN_SIMPLE})/

    private static final Pattern EQUALS = ~/^=?\s*$VERSION_GROUP$/
    private static final Pattern LESS_THAN = ~/<\s*$VERSION_GROUP/
    private static final Pattern GREATER_THAN = ~/(?:>)\s*$VERSION_GROUP/
    private static final Pattern LESS_THAN_EQUAL = ~/(?:<=)\s*$VERSION_GROUP/
    private static final Pattern GREATER_THAN_EQUAL = ~/(?:>=)\s*$VERSION_GROUP/
    private static final Pattern CARET_RANGE = ~/^(?:\^)\s*$VERSION_GROUP$/
    private static final Pattern HYPHEN_RANGE = ~/^$VERSION_GROUP\s*\-\s*$VERSION_GROUP$/

    static Version resolve(String expression, List<Version> versions) {
        List<Version> sortedVersions = versions.toSorted { v1, v2 -> v2 <=> v1 }
        sortedVersions.find { matches(it, expression) }
    }

    static boolean matches(Version version, String expression) {
        expression.tokenize('||')*.trim().any { String simpleExpression ->
            matchesExpression(version, simpleExpression)
        }
    }

    private static boolean matchesExpression(Version version, String expression) {
        List<Boolean> results = []

        if (expression ==~ EQUALS) {
            expression.find(EQUALS) { String match, String versionExpression ->
                results += version == Version.parse(versionExpression)
            }
        }

        if (expression ==~ LESS_THAN) {
            expression.find(LESS_THAN) { String match, String versionExpression ->
                results += version < Version.parse(versionExpression)
            }
        }

        if (expression ==~ GREATER_THAN) {
            expression.find(GREATER_THAN) { String match, String versionExpression ->
                results += version > Version.parse(versionExpression)
            }
        }

        if (expression ==~ LESS_THAN_EQUAL) {
            expression.find(LESS_THAN_EQUAL) { String match, String versionExpression ->
                results += version <= Version.parse(versionExpression)
            }
        }

        if (expression ==~ GREATER_THAN_EQUAL) {
            expression.find(GREATER_THAN_EQUAL) { String match, String versionExpression ->
                results += version >= Version.parse(versionExpression)
            }
        }

        if (expression ==~ HYPHEN_RANGE) {
            expression.find(HYPHEN_RANGE) { String match, String expression1, String expression2 ->
                Version rangeBottom = Version.parse(expression1).floor
                Version rangeTop = Version.parse(expression2).ceiling

                results += (version >= rangeBottom && version <= rangeTop)
            }
        }

        if (expression ==~ CARET_RANGE) {
            expression.find(CARET_RANGE) { String match, String versionExpression ->
                results += matchesCaretRange(version, versionExpression)
            }
        }

        results.every { it }
    }

    private static boolean matchesCaretRange(Version version, String rangeExpression) {
        Version rangeVersion = Version.parse(rangeExpression)

        if (rangeVersion.major != 0) {
            Version ceiling = new Version(major: (rangeVersion.major + 1), minor: 0, patch: 0)
            return (version >= rangeVersion && version < ceiling)
        }
        else if (rangeVersion.minor != 0) {
            Version ceiling = new Version(major: rangeVersion.major, minor: (rangeVersion.minor + 1), patch: 0)
            return (version >= rangeVersion && version < ceiling)
        }
        else {
            return (version == rangeVersion)
        }
    }


}
