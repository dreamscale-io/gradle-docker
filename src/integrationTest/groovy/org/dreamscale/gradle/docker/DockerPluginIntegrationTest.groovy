package org.dreamscale.gradle.docker

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult

class DockerPluginIntegrationTest extends AbstractPluginIntegrationSpecification {

    String containerName = "test"
    String imageName = "postgres"
    DockerSupport dockerSupport

    def setup() {
        dockerSupport = createDockerSupport()
    }

    private DockerSupport createDockerSupport() {
        Project project = ProjectBuilder.builder()
                .withName("project")
                .withProjectDir(projectFS)
                .build()
        new DockerSupport(project)
    }

    def cleanup() {
        removeContainer(containerName)
        "docker rmi ${imageName}:9.5".execute().waitFor()
        "docker rmi ${imageName}:latest".execute().waitFor()
    }

    private void removeContainer(String name) {
        "docker rm -f ${name}".execute().waitFor()
    }

    private void initStandardBuildFile() {
        buildFile << """
apply plugin: 'org.dreamscale.docker'

dockerContainers {
    defaultTag "9.5"
    container {
        name "${containerName}"
        image {
            name "${imageName}"
        }
    }
}
"""
    }

    def "should add docker container lifecycle tasks"() {
        given:
        initStandardBuildFile()

        when:
        run("createTest")

        then:
        assert dockerSupport.isImageLocal(imageName, "9.5")
        assert dockerSupport.isContainerCreated(containerName)
        assert !dockerSupport.isContainerRunning(containerName)
        assert !dockerSupport.isContainerFinished(containerName)

        when:
        run("startTest")

        then:
        assert dockerSupport.isContainerCreated(containerName)
        assert dockerSupport.isContainerRunning(containerName)
        assert !dockerSupport.isContainerFinished(containerName)

        when:
        run("stopTest")

        then:
        assert !dockerSupport.isContainerRunning(containerName)
        assert dockerSupport.isContainerFinished(containerName)
        assert dockerSupport.isImageLocal(imageName, "9.5")

        when: "stopTest is called while the container is not running"
        BuildResult result = run("stopTest")

        then: "stopTest should be skipped"
        assertTaskSkipped(result, "stopTest")

        when: "startTest is used to re-start the task"
        run("startTest")

        then: "the isContainer<action> methods should return correct results"
        assert dockerSupport.isContainerCreated(containerName)
        assert dockerSupport.isContainerRunning(containerName)
        assert !dockerSupport.isContainerFinished(containerName)
    }

    def "should not create/start container if already created/started"() {
        given:
        initStandardBuildFile()

        when:
        run("startTest")

        and:
        BuildResult result = run("startTest")

        then:
        assertTaskSkipped(result, "pullPostgres")
        assertTaskSkipped(result, "startTest")
    }

    private void assertTaskSkipped(BuildResult result, String taskName) {
        assert result.output =~ /(?m)^:${taskName} SKIPPED/
    }

    def "should support referencing an image by name"() {
        String altContainerName = "test-two"
        buildFile << """
apply plugin: 'org.dreamscale.docker'

dockerContainers {
    image {
        name "${imageName}"
    }
    container {
        name "${containerName}"
        imageRef "${imageName}"
    }
    containerSet {
        name "${altContainerName}"
        imageRef "${imageName}"
    }
}
"""

        when:
        run("startTest", "startTestTwo")

        then:
        assert dockerSupport.isImageLocal(imageName, "latest")
        assert dockerSupport.isContainerRunning(containerName)
        assert dockerSupport.isContainerRunning("${altContainerName}1")

        cleanup:
        removeContainer("${altContainerName}1")
    }

    def "dataVolume container should only start once"() {
        given:
        containerName = "hello"
        buildFile << """
apply plugin: 'org.dreamscale.docker'

dockerContainers {
    container {
        name "${containerName}"
        imageName "hello-world"
        dataVolumeContainer()
    }
}
"""

        when:
        run("startHello")

        then:
        assert dockerSupport.isContainerCreated(containerName)
        assert !dockerSupport.isContainerRunning(containerName)
        assert dockerSupport.isContainerFinished(containerName)

        when:
        BuildResult result = run("startHello")

        then:
        assertTaskSkipped(result, "startHello")
        assert dockerSupport.isContainerCreated(containerName)
        assert !dockerSupport.isContainerRunning(containerName)
        assert dockerSupport.isContainerFinished(containerName)
    }

}
