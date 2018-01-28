package org.dreamscale.gradle.docker

import groovy.transform.EqualsAndHashCode
import org.gradle.api.Task

import static DockerAction.*

@EqualsAndHashCode(includes = "config", includeFields = true)
class Image {

    static Image create(ImageConfig config, DockerTaskFactory taskFactory, DockerSupport support) {
        Image image = new Image(config, taskFactory, support)
        image.initialize()
        image
    }


    private ImageConfig config
    private DockerTaskFactory taskFactory
    private DockerSupport support
    Task pullImageTask

    private Image(ImageConfig config, DockerTaskFactory taskFactory, DockerSupport support) {
        this.config = config
        this.taskFactory = taskFactory
        this.support = support
    }

    String getName() {
        config.name
    }

    ImageConfig getConfig() {
        config
    }

    void initialize() {
        initTasks()
    }

    private void initTasks() {
        pullImageTask = createPullImageTask()
    }

    private Task createPullImageTask() {
        Task task = createImageExecTask(PULL_IMAGE, [config.nameAndTag])
        task.onlyIf {
            support.doIgnoreExistingImages() || !support.isImageLocal(config)
        }
        taskFactory.findOrCreateGroupImageTask(PULL_IMAGE).dependsOn(task)
        task
    }

    private Task createImageExecTask(DockerAction action, List additionalArgs) {
        // images may be referenced by multiple tasks, so attempt to find it before creating
        taskFactory.findOrCreateBasicExecEntityTask(config, action, additionalArgs)
    }

    String toString() {
        "Image[name:${name}]"
    }

}
