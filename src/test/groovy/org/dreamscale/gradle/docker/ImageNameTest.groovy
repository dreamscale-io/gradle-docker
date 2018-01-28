package org.dreamscale.gradle.docker

import spock.lang.Specification

class ImageNameTest extends Specification {

    private ImageName.Factory factory = new ImageName.Factory()

    def "should default tag to latest if not tag specified"() {
        when:
        ImageName name = factory.create("username/image")

        then:
        assert name.qualifiedName == "username/image"
        assert name.registryHost == null
        assert name.registryUsername == "username"
        assert name.repositoryName == "image"
        assert name.tag == "latest"
    }

    def "should parse tag is specified"() {
        when:
        ImageName name = factory.create("username/image:tag")

        then:
        assert name.qualifiedName == "username/image"
        assert name.registryHost == null
        assert name.registryUsername == "username"
        assert name.repositoryName == "image"
        assert name.tag == "tag"
    }

    def "should handle registry plus username in image name"() {
        when:
        ImageName name = factory.create("registry/username/image:tag")

        then:
        assert name.qualifiedName == "registry/username/image"
        assert name.registryHost == "registry"
        assert name.registryUsername == "username"
        assert name.repositoryName == "image"
        assert name.tag == "tag"
    }

    def "should use defaultRegistryHost and defaultRegistryUsername"() {
        given:
        factory.defaultRegistryHost = "defaultRegistry"
        factory.defaultRegistryUsername = "defaultUsername"

        when:
        ImageName name = factory.create("image:tag")

        then:
        assert name.qualifiedName == "defaultRegistry/defaultUsername/image"
        assert name.registryHost == "defaultRegistry"
        assert name.registryUsername == "defaultUsername"
        assert name.repositoryName == "image"
        assert name.tag == "tag"
    }

    def "should prefer registry host and username if provided"() {
        given:
        factory.defaultRegistryHost = "defaultRegistry"
        factory.defaultRegistryUsername = "defaultUsername"

        when:
        ImageName name = factory.create("registry/username/image")

        then:
        assert name.qualifiedName == "registry/username/image"
        assert name.registryHost == "registry"
        assert name.registryUsername == "username"
        assert name.repositoryName == "image"
        assert name.tag == "latest"
    }

}
