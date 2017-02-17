package com.craigburke.gradle.client.registry.core

/**
 *
 * Interface for Github credentials
 *
 * @author Søren Glasius
 */
interface GithubCredentials {
    void setGithubUsername(String githubUsername)
    String getGithubUsername()

    void setGithubPassword(String githubPassword)
    String getGithubPassword()

    void setGithubToken(String githubToken)
    String getGithubToken()
}
