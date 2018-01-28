package org.dreamscale.gradle.docker

import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.gradle.api.Task

import static DockerAction.*

@Slf4j
@EqualsAndHashCode(includes = "config", includeFields = true)
class Container {

    static Container create(ContainerConfig config, Image image, DockerTaskFactory taskFactory, DockerSupport support) {
        Container container = new Container(config, image, taskFactory, support)
        container.initialize()
        container
    }


    private Image image
    private ContainerConfig config
    private DockerTaskFactory taskFactory
    private DockerSupport support
    private Task createContainerTask
    private Task removeContainerTask
    private Task startContainerTask
    private Task stopContainerTask
    private Task refreshContainerTask
    private Task restartContainerTask

    Container(ContainerConfig config, Image image, DockerTaskFactory taskFactory, DockerSupport support) {
        this.config = config
        this.image = image
        this.taskFactory = taskFactory
        this.support = support
    }

    String getName() {
        config.name
    }

    void initialize() {
        initTasks()
        initTaskDependencies()
    }

    private void initTasks() {
        createContainerTask = createCreateContainerTask()
        removeContainerTask = createRemoveContainerTask()
        startContainerTask = createStartContainerTask()
        stopContainerTask = createStopContainerTask()
        restartContainerTask = createRestartContainertask()
        if (!config.isDataVolumeContainer()) {
            refreshContainerTask = createRefreshContainertask()
        }
    }

    void addToContainerGroup(String groupName) {
        taskFactory.findOrCreateContainerSetTask(CREATE_CONTAINER, groupName).dependsOn(createContainerTask)
        taskFactory.findOrCreateContainerSetTask(REMOVE_CONTAINER, groupName).dependsOn(removeContainerTask)
        taskFactory.findOrCreateContainerSetTask(START_CONTAINER, groupName).dependsOn(startContainerTask)
        taskFactory.findOrCreateContainerSetTask(STOP_CONTAINER, groupName).dependsOn(stopContainerTask)
    }

    private Task createCreateContainerTask() {
        Task task = createBasicExecTask(CREATE_CONTAINER, config.getCreateContainerArgs())
        task.onlyIf {
            !support.isContainerCreated(config.name)
        }
        task
    }

    private Task createRemoveContainerTask() {
        Task task = createBasicExecTask(REMOVE_CONTAINER, [config.name])
        task.onlyIf {
            support.isContainerCreated(config.name)
        }
        if (!config.isDataVolumeContainer()) {
            taskFactory.findOrCreateGroupContainerTask(REMOVE_CONTAINER).dependsOn(task)
        }
        task
    }

    private Task createStartContainerTask() {
        Task task = createBasicExecTask(START_CONTAINER, [config.name])
        if (!config.isDataVolumeContainer()) {
            task.onlyIf {
                return !support.isContainerRunning(config.name)
            }
        } else {
            task.onlyIf {
                return support.isContainerCreated(config.name) &&
                        !support.isContainerRunning(config.name) &&
                        !support.isContainerFinished(config.name)
            }
            task.doLast {
                logger.quiet("Waiting for data volume container to finish...")
                while (!support.isContainerFinished(config.name)) {
                    sleep(1)
                }
            }
        }
        taskFactory.findOrCreateGroupContainerTask(START_CONTAINER).dependsOn(task)
        task
    }

    private Task createStopContainerTask() {
        Task task = createBasicExecTask(STOP_CONTAINER, [config.name])
        task.onlyIf {
            support.isContainerRunning(config.name)
        }
        taskFactory.findOrCreateGroupContainerTask(STOP_CONTAINER).dependsOn(task)
        task
    }

    private Task createBasicExecTask(DockerAction action, List additionalArgs) {
        taskFactory.createBasicExecEntityTask(config, action, additionalArgs)
    }

    private Task createRefreshContainertask() {
        String taskName = "refresh${config.displayName.capitalize()}"
        String taskDescription = "Remove and re-start ${config.displayName.capitalize()} container"
        Task task = taskFactory.createGroupedFunctionTask(config, taskName, taskDescription, REMOVE_CONTAINER, START_CONTAINER)
        taskFactory.findOrCreateGroupTask("refreshAllContainers", "Remove and re-start all containers").dependsOn(task)
        task
    }

    private Task createRestartContainertask() {
        String taskName = "restart${config.displayName.capitalize()}"
        String taskDescription = "Stop and re-start ${config.displayName.capitalize()} container"
        Task task = taskFactory.createGroupedFunctionTask(config, taskName, taskDescription, STOP_CONTAINER, START_CONTAINER)
        taskFactory.findOrCreateGroupTask("restartAllContainers", "Stop and re-start all containers").dependsOn(task)
        task
    }

    void initTaskDependencies() {
        startContainerTask.dependsOn(createContainerTask)
        startContainerTask.mustRunAfter(stopContainerTask)
        startContainerTask.mustRunAfter(removeContainerTask)

        removeContainerTask.dependsOn(stopContainerTask)

        createContainerTask.dependsOn(image.pullImageTask)
        createContainerTask.mustRunAfter(removeContainerTask)
    }

    void linkToReferencedContainers(DockerEntityRegistry entityRegistry) {
        List<Container> referencedContainers = getReferencedContainers(entityRegistry)

        referencedContainers.each { Container referencedContainer ->
            startContainerTask.dependsOn(referencedContainer.startContainerTask)

            // TODO: if a linked container is created before the linking container, bad things happen, though it
            // looks like it's fixed in trunk (https://github.com/docker/docker/pull/9014/,
            // https://github.com/docker/docker/issues/8796) so should be able to remove this at some point
            createContainerTask.dependsOn(referencedContainer.startContainerTask)
        }
    }

    private List<Container> getReferencedContainers(DockerEntityRegistry entityRegistry) {
        config.referencedContainerNames.findResults { String referencedContainerName ->
            Container container = entityRegistry.getContainer(referencedContainerName)
            if (container == null) {
                log.warn("Failed to resolve container '${referencedContainerName}', referenced by container '${name}")
            }
            container
        }
    }

    String toString() {
        "Container[name:${name}, ${image}]"
    }

}
