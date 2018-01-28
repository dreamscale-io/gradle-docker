package org.dreamscale.gradle.docker

import org.gradle.api.GradleException

class ValidationException extends GradleException {
    ValidationException(String message) {
        super(message)
    }
}
