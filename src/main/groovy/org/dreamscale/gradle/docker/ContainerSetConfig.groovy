package org.dreamscale.gradle.docker

import groovy.transform.ToString
import org.gradle.api.GradleException

@ToString
class ContainerSetConfig {

    private Integer size = 1
    @Delegate
    private ContainerConfig template

    ContainerSetConfig(ImageName.Factory imageNameFactory) {
        template = new ContainerConfig(imageNameFactory)
    }

    String getDisplayName() {
        template.displayName
    }

    void size(int size) {
        if (size < 1) {
            throw new GradleException("Input size must be greater than 0, was=${size}")
        }
        this.size = size
    }

    List<ContainerConfig> getContainerConfigs() {
        List<ContainerConfig> containers = []
        for (int i = 1; i <= size; i++) {
            ContainerConfig container = template.clone()
            container.name = "${template.name}${i}"
            containers << container
        }
        containers
    }

}
