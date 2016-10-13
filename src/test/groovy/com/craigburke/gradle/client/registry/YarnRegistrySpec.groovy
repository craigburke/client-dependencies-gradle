package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.registry.npm.YarnRegistry

class YarnRegistrySpec extends AbstractRegistrySpec {

    def setup() {
        init(YarnRegistry, 'npm')
    }
}
