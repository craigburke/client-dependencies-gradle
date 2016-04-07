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

import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * Semantic version
 * @author Craig Burke
 */
@CompileStatic
class Version implements Comparable<Version>, Cloneable {

    Integer major
    Integer minor
    Integer patch
    String build = ''
    String tag = ''

    static final List<String> XRANGE_VALUES = ['x', 'X', '*']
    static final Pattern PATTERN = ~/v?(?:\d+|x|X|\*)\.?(?:\d+|x|X|\*)?\.?(?:\d+|x|x|X|\*)?(?:\-[^+\s]*)?(?:\+\S*)?/
    static final Pattern PATTERN_GROUPED = ~/v?(\d+|x|X|\*)\.?(\d+|x|X|\*)?\.?(\d+|x|x|X|\*)?(\-[^+\s]*)?(\+\S*)?/

    static Version parse(String expression) {
        Version version = new Version()
        String cleanedExpression = expression.trim() - '='

        cleanedExpression.find(PATTERN_GROUPED) { String match, String major, String minor, String patch,
                                      String tag, String build ->
            version.major = formatTupleMatch(major)
            version.minor = formatTupleMatch(minor)
            version.patch = formatTupleMatch(patch)
            version.tag = tag ? tag - '-' : ''
            version.build = build ? build - '+' : ''
        }

        version
    }

    static Integer formatTupleMatch(String match) {
        String value = match ? (match - '.') : match
        if (!value || XRANGE_VALUES.contains(value)) {
            null
        }
        else {
            Integer.valueOf(value)
        }
    }

    int compareTo(Version other) {
        int value = (major <=> other.major)
        if (value) { return value }

        value = (minor <=> other.minor)
        if (value) { return value }

        value = (patch <=> other.patch)
        if (value) { return value }

        if (tag && other.tag) {
            value = compareTags(tag, other.tag)
        }
        else if (tag || other.tag) {
            value = tag ? -1 : 1
        }

        value
    }

    private int compareTags(String tag1, String tag2) {
        int result = 0

        String[] parts1 = tag1.tokenize('.')
        String[] parts2 = tag2.tokenize('.')

        parts1.eachWithIndex { String part, int index ->
            if (!result) {
                String otherPart = parts2.size() > index ? parts2[index] : null

                if (!otherPart) {
                    result = 1
                }
                else if (part != otherPart) {
                    if (part.isNumber() && otherPart?.isNumber()) {
                        result = Integer.valueOf(part) <=> Integer.valueOf(otherPart)
                    }
                    else {
                        result = part <=> otherPart
                    }
                }
            }
        }

        result
    }

    Version getCeiling() {
        Version ceiling = new Version()

        if (major == null) {
            ceiling.major = 1
        }
        else if (minor == null) {
            ceiling.major = major + 1
        }
        else {
            ceiling.major = major ?: 0
        }

        if (minor == null) {
            ceiling.minor = 0
        }
        else if (patch == null) {
            ceiling.minor = minor + 1
        }
        else {
            ceiling.minor = minor ?: 0
        }

        ceiling.patch = patch ?: 0
        ceiling.tag = tag ?: null

        ceiling
    }

    Version getFloor() {
        Version floor = new Version()
        floor.major = major ?: 0
        floor.minor = minor ?: 0
        floor.patch = patch ?: 0
        floor.tag = tag ?: null
        floor
    }

    String getSimpleVersion() {
        "${formatTuple(major)}.${formatTuple(minor)}.${formatTuple(patch)}"
    }

    static String formatTuple(Integer tuple) {
        tuple == null ? 'x' : tuple
    }

    String getFullVersion() {
        "${simpleVersion}${tag ? '-' : ''}${tag}${build ? '+' : ''}${build}"
    }

    boolean isFuzzy() {
        (major == null || minor == null || patch == null)
    }

    String toString() {
        fullVersion
    }

    Version clone() {
        parse(this.fullVersion)
    }

    boolean compatibleWith(Version version) {
        if (this.tag || version.tag) {
            this == version
        }
        else {
            this.major == version.major
        }
    }

    static List<Version> toList(List<String> versions) {
        versions.collect { parse(it) }
    }
}
