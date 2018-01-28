package org.dreamscale.gradle.docker

import spock.lang.Specification
import spock.lang.Unroll

class ContainerConfigTest extends Specification {

    ContainerConfig container = new ContainerConfig()

    def setup() {
        container.name = "test"
    }

    def "validate should fail if name not set"() {
        given:
        container.name = null

        when:
        container.validate()

        then:
        thrown(ValidationException)
    }

    def "should be equal if name is equal"() {
        expect:
        container == new ContainerConfig(name: "test", imageRef: "image", commandWithArgs: "some command")

        and:
        container != new ContainerConfig(name: "not-test")
    }

    @Unroll
    def "should derive container name from imageName if container name not set"() {
        given:
        container.name = null

        when:
        setImageName.call(container)

        then:
        container.name == "image-name"

        where:
        stub | setImageName
        ""   | { it.imageName("repo/image-name") }
        ""   | { it.image { name "repo/image-name" } }
        ""   | { it.imageRef "repo/image-name"}
    }

    @Unroll
    def "should not derive container name from imageName if container name already set"() {
        when:
        setImageName.call(container)

        then:
        container.name == "test"

        where:
        stub | setImageName
        ""   | { it.imageName("repo/image-name") }
        ""   | { it.image { name "repo/image-name" } }
        ""   | { it.imageRef "repo/image-name"}
    }

}
