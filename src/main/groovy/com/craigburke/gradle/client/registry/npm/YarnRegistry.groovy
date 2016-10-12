package com.craigburke.gradle.client.registry.npm

/**
 *
 * Registry to resolves Yarn NPM Dependencies
 *
 * @author Craig Burke
 */
class YarnRegistry extends NpmRegistry {

    static final String DEFAULT_YARN_URL = 'https://registry.yarnpkg.com'
    static final List<String> DEFAULT_YARN_FILENAMES = ['yarn.json'] + DEFAULT_NPM_FILENAMES

    YarnRegistry(String name, String url = DEFAULT_YARN_URL, List<String> configFiles = DEFAULT_YARN_FILENAMES) {
        super(name, url, configFiles)
    }
}
