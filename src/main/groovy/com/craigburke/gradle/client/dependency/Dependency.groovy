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

class Dependency extends DeclaredDependency {

    Version version
    String downloadUrl
    Dependency parent

    List<Dependency> children = []
    List<String> exclude = []

    void setChildren(List<Dependency> children) {
        children*.parent = this
        this.children = children
    }

    static flattenList(List<Dependency> dependencies) {
        dependencies + dependencies.findAll { it.children }
                .collect { flattenList(it.children) }
                .flatten()
    }

    List<Dependency> getAncestorsAndSelf() {
        collectAncestors(this)
    }

    private static List<Dependency> collectAncestors(Dependency dependency) {
        if (dependency.parent) {
            [dependency] + collectAncestors(dependency.parent)
        }
        else {
            [dependency]
        }
    }

}
