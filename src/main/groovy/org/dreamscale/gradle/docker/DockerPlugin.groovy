package org.dreamscale.gradle.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec

class DockerPlugin implements Plugin<Project> {

    static final String PLUGIN_NAME = "docker"
    static final String LIFECYCLE_GROUP = "Container Lifecycle"

    static DockerPlugin find(Project project) {
        project.plugins.findPlugin(PLUGIN_NAME)
    }


    private Project project
    boolean ignoreExistingImages = false

    @Override
    void apply(Project project) {
        this.project = project
        addDockerExtension()
        addIgnoreExistingImagesTask()
    }

    private void addDockerExtension() {
        project.extensions.create(DockerExtension.NAME, DockerExtension, project)
   	}

    private void addIgnoreExistingImagesTask() {
        Task ignoreExistingImagesTask = project.tasks.create("ignoreExistingImages") {
            group LIFECYCLE_GROUP
            doLast {
                ignoreExistingImages = true
            }
        }

        project.afterEvaluate {
            project.tasks.withType(Exec) { Task task ->
                if (task.group == LIFECYCLE_GROUP) {
                    task.mustRunAfter ignoreExistingImagesTask
                }
            }
        }
    }

}
