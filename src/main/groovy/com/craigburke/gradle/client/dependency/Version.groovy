package com.craigburke.gradle.client.dependency

import java.util.regex.Pattern

class Version implements Comparable<Version> {

    int major
    int minor
    int patch
    String build
    String tag

    Version(String expression) {
        Pattern semVersion = ~/(\d*)\.(\d*)\.(\d*)(\-[^+]*)?(\+.*)?/

        expression.find(semVersion) { String match, String major, String minor, String patch,
                                      String tag, String build ->

            this.major = Integer.valueOf(major)
            this.minor = Integer.valueOf(minor)
            this.patch = Integer.valueOf(patch)
            this.tag = tag ? tag - '-' : ''
            this.build = build ? build - '+' : ''
        }
    }

    int compareTo(Version other) {
        ((major <=> other.major) ?: (minor <=> other.minor)) ?: (patch <=> other.patch)
    }

    String getSimpleVersion() {
        "${major}.${minor}.${patch}"
    }

    String getFullVersion() {
        "${simpleVersion}${tag ? '-' : ''}${tag}${build ? '+' : ''}${build}"
    }

    String toString() {
        fullVersion
    }

}
