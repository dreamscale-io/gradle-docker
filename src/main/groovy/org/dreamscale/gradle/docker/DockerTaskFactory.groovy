package org.dreamscale.gradle.docker

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec

class DockerTaskFactory {

    private Project project
    private DockerSupport support

    DockerTaskFactory(Project project) {
        this.project = project
        this.support = new DockerSupport(project)
    }

    Task findOrCreateGroupImageTask(DockerAction action) {
        findOrCreateGroupActionTask(action, "images")
    }

    Task findOrCreateGroupContainerTask(DockerAction action) {
        findOrCreateGroupActionTask(action, "containers")
    }

    Task findOrCreateContainerSetTask(DockerAction action, String containerSetName) {
        String taskName = "${action.displayName}${containerSetName.capitalize()}"
        String taskDescription = "${action.displayName.capitalize()} all ${containerSetName} containers"
        findOrCreateGroupTask(taskName, taskDescription)
    }

    private Task findOrCreateGroupActionTask(DockerAction action, String typeName) {
        String taskName = "${action.displayName}All${typeName.capitalize()}"
        String taskDescription = "${action.displayName.capitalize()} all ${typeName}"
        findOrCreateGroupTask(taskName, taskDescription)
    }

    Task findOrCreateGroupTask(String taskName, String taskDescription) {
        project.tasks.findByName(taskName) ?: createGroupTask(taskName, taskDescription)
    }

    private Task createGroupTask(String taskName, String taskDescription) {
        Task task = project.tasks.create(taskName)
        task.group = DockerPlugin.LIFECYCLE_GROUP
        task.description = taskDescription
        task
    }

    Task createGroupedFunctionTask(EntityConfig entity, String taskName, String taskDescription, DockerAction ... actions) {
        Task container = project.tasks.create(taskName)
        container.group = DockerPlugin.LIFECYCLE_GROUP
        for (DockerAction action : actions) {
            container.dependsOn(entity.createTaskName(action))
        }
        container.description = taskDescription
        container
    }

    Task findOrCreateBasicExecEntityTask(EntityConfig entity, DockerAction action, List additionalArgs) {
        String taskName = entity.createTaskName(action)
        project.tasks.findByName(taskName) ?: createBasicExecEntityTask(entity, action, additionalArgs)
    }

    Task createBasicExecEntityTask(EntityConfig entity, DockerAction action, List additionalArgs) {
        String taskName = entity.createTaskName(action)
        Exec task = project.tasks.create(taskName, Exec)

        task.group = DockerPlugin.LIFECYCLE_GROUP
        task.description = "${action.displayName.capitalize()} the ${entity.displayName.capitalize()} ${entity.typeName}"
        task.executable = "docker"
        task.args(action.name)
        task.args(additionalArgs)
        task.doFirst {
            project.logger.quiet("${task.getCommandLine().join(" ")}")
        }
        task
    }

}
