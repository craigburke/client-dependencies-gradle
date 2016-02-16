package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.dependency.Version

interface Registry {
    File getSourceFolder(Dependency dependency)
    List<Version> getVersionList(DeclaredDependency declaredDependency)
    Dependency loadDependency(DeclaredDependency declaredDependency)

    void setInstallPath(String installPath)
    String getInstallPath()

    void setCachePath(String cachePath)
}
