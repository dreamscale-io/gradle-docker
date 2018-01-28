package org.dreamscale.gradle.docker

interface EntityConfig {

    String getName()

    String getDisplayName()

    String getTypeName()

    String createTaskName(DockerAction action)

}
