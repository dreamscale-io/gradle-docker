package org.dreamscale.gradle.docker

import com.bancvue.gradle.test.AbstractPluginSpecification
import org.gradle.api.Task
import org.gradle.api.tasks.Exec

class DockerPluginTest extends AbstractPluginSpecification {

    @Override
    String getPluginName() {
        DockerPlugin.PLUGIN_NAME
    }

    def setup() {
        applyPlugin()
    }

    private DockerEntityRegistry getEntityRegistry() {
        DockerExtension.DockerExtensionProcessor.entityRegistryTestAccessor
    }

    private Image getImage(String name) {
        getEntityRegistry().getImage(name)
    }

    private void assertImageExists(String name) {
        Image image = getEntityRegistry().getImage(name)
        assert image: "Image with name=${name} does not exists, all images=${getEntityRegistry().getImages()}"
    }

    private Container getContainer(String name) {
        getEntityRegistry().getContainer(name)
    }

    private void assertContainerExists(String name) {
        Container container = getEntityRegistry().getContainer(name)
        assert container: "Container with name=${name} does not exists, all containers=${getEntityRegistry().getContainers()}"
    }

    def "containerSet should create multiple containers"() {
        when:
        project.dockerContainers {
            containerSet {
                name "test-container"
                size 2
                image {
                    name "hello-world"
                }
            }
        }
        evaluateProject()

        then:
        assertTasksDefined("startTestContainer1", "stopTestContainer1")
        assertTasksDefined("startTestContainer2", "stopTestContainer2")
        assertTasksDefined("startTestContainer", "stopTestContainer")
        assertTaskDependency("startTestContainer", "startTestContainer1", "startTestContainer2")
        assertTaskDependency("stopTestContainer", "stopTestContainer1", "stopTestContainer2")
    }

    def "should support referencing an image by name"() {
        when:
        project.dockerContainers {
            image {
                name "test-image"
            }
            container {
                name "test-one"
                imageRef "test-image"
            }
            containerSet {
                name "test-two"
                imageRef "test-image"
            }
        }
        evaluateProject()

        then:
        assertTasksDefined("pullTestImage",
                "startTestOne", "stopTestOne",
                "startTestTwo1", "stopTestTwo1",
        )
    }

    def "should add group tasks to operate on all images/containers"() {
        when:
        project.dockerContainers {
            container {
                name "test"
                image {
                    name "test-image"
                }
            }
        }
        evaluateProject()

        then:
        assertTasksDefined(
                "pullAllImages",
                "startAllContainers", "stopAllContainers",
                "removeAllContainers", "refreshAllContainers", "restartAllContainers"
        )
        assertTaskDependency("pullAllImages", "pullTestImage")
        assertTaskDependency("startAllContainers", "startTest")
        assertTaskDependency("stopAllContainers", "stopTest")
        assertTaskDependency("removeAllContainers", "removeTest")
        assertTaskDependency("refreshAllContainers", "refreshTest")
        assertTaskDependency("restartAllContainers", "restartTest")
    }

    def "should only add grouped image tasks if only defining an image"() {
        when:
        project.dockerContainers {
            image {
                name "test-image"
            }
        }
        evaluateProject()

        then:
        assertTasksDefined(
                "pullAllImages"
        )

        and:
        assertTasksNotDefined(
                "startAllContainers", "stopAllContainers",
                "removeAllContainers", "refreshAllContainers", "restartAllContainers"
        )
    }

    def "should not add grouped tasks if no image or container defined"() {
        when:
        project.dockerContainers {}
        evaluateProject()

        then:
        assertTasksNotDefined(
                "pullAllImages",
                "startAllContainers", "stopAllContainers",
                "removeAllContainers", "refreshAllContainers", "restartAllContainers"
        )
        assert project.tasks.withType(Exec).size() == 0
    }

    def "should not add container to removeAllContainers task if data volume container"() {
        when:
        project.dockerContainers {
            container {
                name "data"
                dataVolumeContainer()
            }
        }
        evaluateProject()

        then:
        assertTasksDefined("startData", "stopData", "createData", "removeData")
        assertTaskDependency("startAllContainers", "startData")
        assertTaskDependency("stopAllContainers", "stopData")
        assertTasksNotDefined("removeAllContainers")
    }

    def "should handle tags in image name"() {
        when:
        project.dockerContainers {
            image {
                name "dreamscale/test-image:1.1"
            }
        }
        evaluateProject()

        then:
        assertTasksDefined("pullTestImage")
    }

    def "should translate container options to command line arguments"() {
        when:
        project.dockerContainers {
            container {
                name "container"
                hostname "localhost"
                link "other-container"
                publish "80"
                publish "8080:8080"
                volume "/local/dir:/container/dir"
                volumesFrom "volume-container"
                commandWithArgs "/usr/local/bin/doit.sh arg1 arg2"
            }
        }
        evaluateProject()

        then:
        Exec task = project.tasks.getByName("createContainer")
        assert task.commandLine.contains("--name=container")
        assert task.commandLine.contains("--hostname=localhost")
        assert task.commandLine.contains("--link=other-container")
        assert task.commandLine.contains("--publish=80")
        assert task.commandLine.contains("--publish=8080:8080")
        assert task.commandLine.contains("--volume=/local/dir:/container/dir")
        assert task.commandLine.contains("--volumes-from=volume-container")

        and:
        assert task.commandLine.contains("/usr/local/bin/doit.sh")
        assert task.commandLine.contains("arg1")
        assert task.commandLine.contains("arg2")
        task.commandLine.join(" ").endsWith("/usr/local/bin/doit.sh arg1 arg2")
    }

    def "should link to referenced container tasks"() {
        when:
        project.dockerContainers {
            container {
                name "base"
            }
            container {
                name "linked"
                link "base:bs"
            }
            container {
                name "linked-by-volume"
                volumesFrom "linked"
            }
            container {
                name "depends-reference"
                dependsOn "linked-by-volume"
            }
            container {
                name "unknown-reference"
                link "unknown:un"
            }
        }
        evaluateProject()

        then:
        assertTaskDependency("startLinked", "startBase")
        assertTaskDependency("startLinkedByVolume", "startLinked")
        assertTaskDependency("startDependsReference", "startLinkedByVolume")
        assertNoTaskDependency("startUnknownReference", "startUnknown")

        // TODO: this should be removed in future versions of docker
        and: "linked containers are started before the linking container is created (docker bug #8796)"
        assertTaskDependency("createLinked", "startBase")
        assertTaskDependency("createLinkedByVolume", "startLinked")
        assertTaskDependency("createDependsReference", "startLinkedByVolume")
        assertNoTaskDependency("createUnknownReference", "startUnknown")
    }

    def "should incorporate defaultRegistryHost, defaultRegistryUsername and defaultTag into image names"() {
        when:
        project.dockerContainers {
            defaultTag "1.0"
            defaultRegistryHost "host"
            defaultRegistryUsername "username"

            image {
                name "image"
            }
            container {
                imageName "containerImage"
            }
            container {
                name "container"
            }
        }

        then:
        Image image = getImage("host/username/image")
        assert image.name == "host/username/image"
        assert image.config.shortName == "image"
        assert image.config.nameAndTag == "host/username/image:1.0"

        and:
        Container containerImage = getContainer("containerImage")
        assert containerImage.name == "containerImage"
        assert containerImage.image.name == "host/username/containerImage"
        assert containerImage.image.config.shortName == "containerImage"
        assert containerImage.image.config.nameAndTag == "host/username/containerImage:1.0"

        and:
        Container container = getContainer("container")
        assert container.name == "container"
        assert container.image.name == "host/username/container"
        assert container.image.config.shortName == "container"
        assert container.image.config.nameAndTag == "host/username/container:1.0"
    }

    def "should be able to reference dockerhub image if defaultRegistryHost is specified"() {
        when:
        project.dockerContainers {
            defaultRegistryHost "host"
            defaultRegistryUsername "username"
            container {
                name "container"
                imageName "/otherUsername/container"
            }
        }
        evaluateProject()

        then:
        assertImageExists("otherUsername/container")
        assertContainerExists("container")
    }
}
