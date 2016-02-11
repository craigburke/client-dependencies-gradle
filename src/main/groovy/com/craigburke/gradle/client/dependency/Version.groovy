package com.craigburke.gradle.client.dependency

import java.util.regex.Pattern

class Version implements Comparable<Version> {

    Integer major
    Integer minor
    Integer patch
    String build
    String tag

    static final List<String> X_RANGE_VALUES = ['x', 'X', '*']
    static final Pattern VERSION_PATTERN = ~/^(\d*|x|X|\*)?\.?(\d*|x|X|\*)?\.?(\d*|x|x|X|\*)?(\-[^+]*)?(\+.*)?$/

    Version(String expression) {

        expression.find(VERSION_PATTERN) { String match, String major, String minor, String patch,
                                      String tag, String build ->

            this.major = formatTupleMatch(major)
            this.minor = formatTupleMatch(minor)
            this.patch = formatTupleMatch(patch)
            this.tag = tag ? tag - '-' : ''
            this.build = build ? build - '+' : ''
        }
    }

    private Integer formatTupleMatch(String match) {
        String value = match ? (match - '.') : match
        if (!value || X_RANGE_VALUES.contains(value)) {
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

        if (tag || other.tag) {
            value = (tag <=> other.tag)
        }

        value
    }

    String getSimpleVersion() {
        "${formatTuple(major)}.${formatTuple(minor)}.${formatTuple(patch)}"
    }

    String formatTuple(Integer tuple) {
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

    static List<Version> toList(List<String> versions) {
        versions.collect { new Version(it) }
    }
}
