package org.dreamscale.gradle.docker

interface DockerEntityRegistry {

    Image getImage(String name)

    Container getContainer(String name)

    Set<Image> getImages()

    Set<Container> getContainers()

}
