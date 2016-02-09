package com.craigburke.gradle.client.registry

class BowerRegistrySpec extends AbstractRegistrySpec {

    final static String GIT_URL_PLACEHOLDER = '$GIT_URL_BASE'

    def setup() {
        setupRegistry(BowerRegistry)
        String gitUrl = "file://${resource('bower').path}"

        responses = [
                '/packages/foo'          : resource('bower/foo.json').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/packages/bar'          : resource('bower/bar.json').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/packages/baz'          : resource('bower/baz.json').text.replace(GIT_URL_PLACEHOLDER, gitUrl),
                '/packages/foobar'       : resource('bower/foobar.json').text.replace(GIT_URL_PLACEHOLDER, gitUrl)
        ]
    }
}
