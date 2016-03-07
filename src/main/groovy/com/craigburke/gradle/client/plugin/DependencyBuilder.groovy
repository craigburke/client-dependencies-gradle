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
package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.registry.core.Registry

/**
 *
 * Builds list of simple dependencies from the plugin DSL
 *
 * @author Craig Burke
 */
class DependencyBuilder {

    Registry registry
    List<Dependency> rootDependencies = []

    DependencyBuilder(Registry registry) {
        this.registry = registry
    }

    def methodMissing(String name, args) {
        Map props = [:]
        props.registry = registry
        props.name = name
        props.versionExpression = args.find { it instanceof String }

        Map additionalProps = args.find { it instanceof Map }
        if (additionalProps) {
            props += additionalProps
        }

        Dependency dependency = new Dependency(props)

        if (args && args.last() instanceof Closure) {
            dependency.copyConfig = args.last()
        }

        rootDependencies += dependency
    }

}
