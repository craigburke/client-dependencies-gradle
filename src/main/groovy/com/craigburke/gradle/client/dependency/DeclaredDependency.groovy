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

import com.craigburke.gradle.client.registry.Registry
import groovy.transform.CompileStatic

/**
 * Simple base dependency
 * @author Craig Burke
 */
@CompileStatic
class DeclaredDependency {

    Registry registry
    String name
    String versionExpression
    String url

    List<String> exclude = []
    boolean transitive = true
    Closure copyConfig

    void setExclude(String exclude) {
        this.exclude = [exclude]
    }

    void setExclude(List<String> exclude) {
        this.exclude = exclude
    }
}
