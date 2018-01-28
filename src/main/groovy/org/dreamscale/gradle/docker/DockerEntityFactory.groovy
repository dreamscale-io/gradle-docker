package org.dreamscale.gradle.docker

import org.gradle.api.Project

class DockerEntityFactory implements DockerEntityRegistry {

    private Project project
    private DockerSupport support
    private DockerTaskFactory taskFactory
    private Map<String,Image> imageMap = [:]
    private Map<String,Container> containerMap = [:]

    DockerEntityFactory(Project project) {
        this.project = project
        this.support = new DockerSupport(project)
        this.taskFactory = new DockerTaskFactory(project)
    }

    DockerEntityRegistry getEntityRegistry() {
        this
    }

    @Override
    Image getImage(String name) {
        imageMap[name]
    }

    @Override
    Container getContainer(String name) {
        containerMap[name]
    }

    @Override
    Set<Image> getImages() {
        imageMap.values()
    }

    @Override
    Set<Container> getContainers() {
        containerMap.values()
    }

    Image createImage(ImageConfig config) {
        config.validate()
        Image image = Image.create(config, taskFactory, support)
        imageMap[config.name] = image
        image
    }

    Container createContainer(ContainerConfig config, Image image) {
        config.validate()
        Container container = Container.create(config, image, taskFactory, support)
        containerMap[config.name] = container
        container
    }

}
