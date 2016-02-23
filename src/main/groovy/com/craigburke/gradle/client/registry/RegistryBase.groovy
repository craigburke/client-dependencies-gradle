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

import jsr166y.ForkJoinPool

class RegistryBase {

    String registryUrl
    String cachePath
    String installPath

    static ForkJoinPool pool

    RegistryBase(String url) {
        registryUrl = url
    }

    static void setThreadPoolSize(int poolSize) {
        pool = new ForkJoinPool(poolSize)
    }

    static String formatPath(String path) {
        path.replace('\\', '/').replace('//', '/')
    }

    String getMainFolderPath(String dependencyName) {
        formatPath("${cachePath}/${dependencyName}")
    }

}