package org.dreamscale.gradle.docker

import spock.lang.Specification

class ContainerTest extends Specification {

    def "should equal if config is equal"() {
        given:
        ContainerConfig config1 = new ContainerConfig(name: "config-one")
        ContainerConfig config2 = new ContainerConfig(name: "config-two")

        expect:
        createContainer(config1) == createContainer(config1)

        and:
        createContainer(config1) != createContainer(config2)
    }

    private Container createContainer(ContainerConfig config) {
        new Container(config, null, null, null)
    }

    def "toString should include container and image name"() {
        given:
        Container container = new Container(
                new ContainerConfig(name: "container"),
                new Image(new ImageConfig(name: "image"), null, null),
                null,
                null
        )

        expect:
        container.toString() == "Container[name:container, Image[name:image]]"
    }

}
