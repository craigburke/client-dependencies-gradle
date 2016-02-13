package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version

interface Registry {

    File getSourceFolder(Dependency dependency)
    List<Version> getVersionList(SimpleDependency simpleDependency)
    Dependency loadDependency(SimpleDependency simpleDependency)

    void setInstallPath(String installPath)
    String getInstallPath()

    void setCachePath(String cachePath)
}
