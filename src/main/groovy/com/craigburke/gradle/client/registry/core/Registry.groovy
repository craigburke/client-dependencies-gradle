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
package com.craigburke.gradle.client.registry.core

import com.craigburke.gradle.client.dependency.Dependency
import groovy.transform.CompileStatic

/**
 *
 * Registry interface
 *
 * @author Craig Burke
 */
@CompileStatic
interface Registry extends GithubCredentials {
    void setName(String name)
    String getName()

    void setOffline(boolean offline)
    boolean getOffline()

    void setInstallDir(File installDir)
    File getInstallDir()

    void setLocalCacheDir(File cacheDir)
    File getLocalCacheDir()

    void setGlobalCacheDir(File cacheDir)
    File getGlobalCacheDir()

    void setUseGlobalCache(boolean useGlobalCache)
    boolean getUseGlobalCache()

    void setCheckDownloads(boolean checkDownloads)
    boolean getCheckDownloads()

    void setGithubUsername(String githubUsername)
    String getGithubUsername()

    void setGithubPassword(String githubPassword)
    String getGithubPassword()

    void setGithubToken(String githubToken)
    String getGithubToken()

    void setUserAgent(String userAgent)
    String getUserAgent()

    String getUrl()

    Dependency loadDependency(Dependency dependency, Dependency parent)
    Resolver getResolver(Dependency dependency)
}
