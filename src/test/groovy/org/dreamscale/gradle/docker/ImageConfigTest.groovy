package org.dreamscale.gradle.docker

import spock.lang.Specification

class ImageConfigTest extends Specification {

    ImageConfig image = new ImageConfig()

    def setup() {
        image.name("test")
    }

    def "validate should fail if imageName not set"() {
        given:
        image.imageName = null

        when:
        image.validate()

        then:
        thrown(ValidationException)
    }

    def "name should return image repo name"() {
        given:
        image.name("test:tag")

        expect:
        assert image.name == "test"
    }

}
