package org.dreamscale.gradle.docker

import groovy.util.logging.Slf4j
import org.gradle.api.Project

@Slf4j
class DockerSupport {

    private Project project

    DockerSupport(Project project) {
        this.project = project
    }

    String getProjectPropertyOrDefault(String property, String defaultValue) {
        project.hasProperty(property) ? project.property(property) : defaultValue
    }

    boolean doIgnoreExistingImages() {
        DockerPlugin.find(project).ignoreExistingImages
    }

    boolean isImageLocal(ImageConfig image) {
        isImageLocal(image.name, image.tag)
    }

    boolean isImageLocal(String repoName, String tagName) {
        inspect("${repoName}:${tagName}", "{{.Architecture}}")
    }

    boolean isContainerCreated(String containerName) {
        inspect("${containerName}", "{{.State.StartedAt}}")
    }

    boolean isContainerRunning(String containerName) {
        inspect("${containerName}", "{{.State.Running}}") == "true"
    }

    boolean isContainerFinished(String containerName) {
        String finishedAt = inspect("${containerName}", "{{.State.FinishedAt}}")
        (finishedAt && finishedAt.startsWith("0001-01-01") == false) && !isContainerRunning(containerName)
    }

    private String inspect(String name, String format) {
        String output = null
        new ByteArrayOutputStream().withStream { es ->
            new ByteArrayOutputStream().withStream { os ->
                project.exec {
                    commandLine "docker", "inspect", "-f", format, name
                    errorOutput = es
                    standardOutput = os
                    ignoreExitValue = true
                }
                output = os.toString().trim()
            }
        }
        log.debug("docker inspect -f ${format} ${name}, output=${output}")
        output == "<no value>" ? null : output
    }

}
