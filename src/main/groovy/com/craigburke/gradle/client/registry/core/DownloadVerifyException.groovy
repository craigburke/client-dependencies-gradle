package com.craigburke.gradle.client.registry.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 *
 * Exception when a download is invalid
 *
 * @author Craig Burke
 */
@InheritConstructors
@CompileStatic
class DownloadVerifyException extends RuntimeException {
}
