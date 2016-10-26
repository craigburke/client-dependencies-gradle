package com.craigburke.gradle.client.registry.core

interface GithubCredentials {
    void setGithubUsername(String githubUsername)
    String getGithubUsername()

    void setGithubPassword(String githubPassword)
    String getGithubPassword()


}