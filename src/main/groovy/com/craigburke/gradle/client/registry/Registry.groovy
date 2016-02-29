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
package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.dependency.Version
import groovy.transform.CompileStatic

@CompileStatic
interface Registry {
    File getSourceFolder(Dependency dependency)
    List<Version> getVersionList(DeclaredDependency declaredDependency)
    Dependency loadDependency(DeclaredDependency declaredDependency, Dependency parent)

    void setInstallPath(String installPath)
    String getInstallPath()

    void setCachePath(String cachePath)

    String getUrl()
}
