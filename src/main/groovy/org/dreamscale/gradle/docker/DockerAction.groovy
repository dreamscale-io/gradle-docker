package org.dreamscale.gradle.docker

enum DockerAction {
    PULL_IMAGE("pull"),
    PUSH_IMAGE("push"),
    START_CONTAINER("start"),
    STOP_CONTAINER("stop"),
    CREATE_CONTAINER("create"),
    REMOVE_CONTAINER("rm", "remove")

    String name
    String displayName

    private DockerAction(String name) {
        this(name, name)
    }

    private DockerAction(String name, String displayName) {
        this.name = name
        this.displayName = displayName
    }

    String getName() {
        name
    }

    String getDisplayName() {
        displayName
    }
}
