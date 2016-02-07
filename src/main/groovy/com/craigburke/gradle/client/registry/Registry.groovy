package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import com.craigburke.gradle.client.dependency.Version

interface Registry {
    File getInstallSource(Dependency dependency)
    List<Version> getVersionList(String dependencyName)
    Dependency loadDependency(SimpleDependency simpleDependency)
    String getSourceIncludeExpression(String sourceExpression)

    void setInstallPath(String installPath)
    String getInstallPath()

    void setCachePath(String cachePath)

    String getSourcePathPrefix()
}
