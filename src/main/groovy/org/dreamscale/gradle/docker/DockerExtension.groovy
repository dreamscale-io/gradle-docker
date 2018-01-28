package org.dreamscale.gradle.docker

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class DockerExtension {

    static final String NAME = "dockerContainers"

    private Project project
    private DockerExtensionProcessor processor
    private ImageName.Factory imageNameFactory

    DockerExtension(Project project) {
        this.project = project
        this.imageNameFactory = new ImageName.Factory()
        this.processor = new DockerExtensionProcessor(project)

        DockerExtensionProcessor extensionProcessor = processor
        project.afterEvaluate {
            extensionProcessor.postProcessEntities()
        }
    }

    void defaultRegistryHost(String defaultRegistryHost) {
        imageNameFactory.defaultRegistryHost = defaultRegistryHost
    }

    void defaultRegistryUsername(String defaultRegistryUsername) {
        imageNameFactory.defaultRegistryUsername = defaultRegistryUsername
    }

    void defaultTag(String defaultTag) {
        imageNameFactory.defaultTag = defaultTag
    }

    void image(Closure closure) {
        ImageConfig image = new ImageConfig(imageNameFactory)
        ConfigureUtil.configure(closure, image)
        processor.addImage(image)
    }

    void container(Closure closure) {
        ContainerConfig container = new ContainerConfig(imageNameFactory)
        ConfigureUtil.configure(closure, container)
        processor.addContainer(container)
    }

    void containerSet(Closure closure) {
        ContainerSetConfig containerSet = new ContainerSetConfig(imageNameFactory)
        ConfigureUtil.configure(closure, containerSet)
        processor.addContainerSet(containerSet)
    }


    private static final class DockerExtensionProcessor {

        // used by unit tests to access the internally-created images/container; necessary since the
        // gradle wraps the extension which prevents accessing internal variables
        private static DockerEntityRegistry entityRegistryTestAccessor

        private DockerEntityFactory entityFactory
        private DockerEntityRegistry entityRegistry

        DockerExtensionProcessor(Project project) {
            this.entityFactory = new DockerEntityFactory(project)
            this.entityRegistry = entityFactory.entityRegistry
            entityRegistryTestAccessor = entityRegistry
        }

        Image addImage(ImageConfig imageConfig) {
            Image image = entityRegistry.getImage(imageConfig.name)
            if (!image) {
                image = entityFactory.createImage(imageConfig)
            }
            image
        }

        void addContainerSet(ContainerSetConfig containerSet) {
            setImageIfRefDefined(containerSet)
            Image image = addImage(containerSet.imageConfig)

            for (ContainerConfig containerConfig : containerSet.getContainerConfigs()) {
                Container container = entityFactory.createContainer(containerConfig, image)
                container.addToContainerGroup(containerSet.displayName)
            }
        }

        void addContainer(ContainerConfig containerConfig) {
            setImageIfRefDefined(containerConfig)
            Image image = addImage(containerConfig.imageConfig)

            entityFactory.createContainer(containerConfig, image)
        }

        private void setImageIfRefDefined(def container) {
            String imageRef = container.imageRef
            if (imageRef) {
                Image image = entityRegistry.getImage(imageRef)
                if (!image) {
                    throw new GradleException("Image referenced but not yet defined, imageRef=${imageRef}, container=${container}")
                }
                container.imageConfig = image.config
            }
        }

        void postProcessEntities() {
            postProcessContainers()
        }

        private void postProcessContainers() {
            entityRegistry.getContainers().each { Container container ->
                container.linkToReferencedContainers(entityRegistry)
            }
        }

    }

}
