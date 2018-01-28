package org.dreamscale.gradle.docker

import spock.lang.Specification

class ImageTest extends Specification {

    def "should equal if config is equal"() {
        given:
        ImageConfig config1 = new ImageConfig(name: "config-one")
        ImageConfig config2 = new ImageConfig(name: "config-two")

        expect:
        createImage(config1) == createImage(config1)

        and:
        createImage(config1) != createImage(config2)
    }

    private Image createImage(ImageConfig config) {
        new Image(config, null, null)
    }

    def "toString should include image name"() {
        given:
        Image image = new Image(new ImageConfig(name: "image"), null, null)

        expect:
        image.toString() == "Image[name:image]"
    }

}
