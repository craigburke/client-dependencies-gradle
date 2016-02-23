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

        if (expression?.trim() in ['latest', '*', '']) {
            sortedVersions.first()
        }
        else {
            sortedVersions.find { matches(it, expression) }
        }
    }

    static boolean matches(Version version, String expression) {
        expression.tokenize('||')*.trim().any { String simpleExpression ->
            matchesExpression(version, simpleExpression)
        }
    }

    private static boolean matchesExpression(Version version, String expression) {
        List<Boolean> results = []

        expression.find(EQUALS) { String match, String versionExpression ->
            Version matchedVersion = Version.parse(versionExpression)

            if (matchedVersion.fuzzy) {
                results += (version >= matchedVersion.floor && version < matchedVersion.ceiling)
            }
            else {
                results += version == matchedVersion
            }
        }

        expression.find(LESS_THAN) { String match, String versionExpression ->
            Version matchedVersion = Version.parse(versionExpression)

            if (!matchedVersion.fuzzy) {
                results += version < matchedVersion
            }
        }

        expression.find(GREATER_THAN) { String match, String versionExpression ->
            Version matchedVersion = Version.parse(versionExpression)

            if (!matchedVersion.fuzzy) {
                results += version >matchedVersion
            }
        }

        expression.find(LESS_THAN_EQUAL) { String match, String versionExpression ->
            Version matchedVersion = Version.parse(versionExpression)

            if (!matchedVersion.fuzzy) {
                results += version <= matchedVersion
            }
        }

        expression.find(GREATER_THAN_EQUAL) { String match, String versionExpression ->
            Version matchedVersion = Version.parse(versionExpression)

            if (!matchedVersion.fuzzy) {
                results += version >= matchedVersion
            }
        }

        expression.find(HYPHEN_RANGE) { String match, String expression1, String expression2 ->
            Version rangeBottom = Version.parse(expression1)
            Version rangeTop = Version.parse(expression2)

            results += (version >= rangeBottom.floor && (rangeTop.fuzzy ? (version < rangeTop.ceiling) : (version <= rangeTop)))
        }

        expression.find(CARET_RANGE) { String match, String versionExpression ->
            results += matchesCaretRange(version, versionExpression)
        }

        if (version.tag && !expression.matches(EQUALS)) {
            false
        }
        else {
            results.every { it }
        }
    }

    private static boolean matchesCaretRange(Version version, String rangeExpression) {
        Version matchedVersion = Version.parse(rangeExpression)
        Version rangeBottom = matchedVersion.floor
        Version rangeTop

        if (matchedVersion.major) {
            rangeTop = new Version(major: matchedVersion.major + 1, minor: 0, patch: 0)
        }
        else if (matchedVersion.minor) {
            rangeTop = new Version(major: matchedVersion.major, minor: matchedVersion.minor + 1, patch: 0)
        }
        else {
            rangeTop = new Version(major: matchedVersion.major, minor: matchedVersion.minor, patch: matchedVersion.patch + 1)
        }

        (version >= rangeBottom && version < rangeTop)
    }


}
