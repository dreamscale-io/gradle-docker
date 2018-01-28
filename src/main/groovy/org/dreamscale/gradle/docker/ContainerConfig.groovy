package org.dreamscale.gradle.docker

import com.google.common.base.CaseFormat
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.util.ConfigureUtil

@ToString
@EqualsAndHashCode(includes = "name")
class ContainerConfig implements EntityConfig, Cloneable {

    static final String TYPE_NAME = "container"

    String name
    String imageRef
    ImageConfig imageConfig
    String commandWithArgs
    boolean dataVolumeContainer
    private List<String> options = []
    private List<String> referencedContainerNames = []
    private ImageName.Factory imageNameFactory

    // for testing only
    private ContainerConfig() {
        this(new ImageName.Factory())
    }

    ContainerConfig(ImageName.Factory imageNameFactory) {
        this.imageNameFactory = imageNameFactory
        this.imageConfig = new ImageConfig(imageNameFactory)
    }

    void name(String name) {
        this.name = name
        if (!imageConfig.name) {
            imageConfig.name(name)
        }
    }

    String getDisplayName() {
        CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name)
    }

    String getTypeName() {
        TYPE_NAME
    }

    void createArgs(String ... createArgs) {
        this.createArgs.addAll(createArgs)
    }

    void image(Closure closure) {
        ConfigureUtil.configure(closure, imageConfig)
        setNameIfNotSet(imageConfig.shortName)
    }

    private void setNameIfNotSet(String name) {
        if (!this.name) {
            this.name = name
        }
    }

    void imageName(String name) {
        imageConfig.name(name)
        setNameIfNotSet(imageConfig.shortName)
    }

    void buildDir(String buildDirPath) {
        imageConfig.buildDir(buildDirPath)
    }

    void buildDir(File buildDir) {
        imageConfig.buildDir(buildDir)
    }

    void imageRef(String imageRef) {
        this.imageRef = imageRef
        setNameIfNotSet(imageNameFactory.create(imageRef).repositoryName)
    }

    String getImageNameAndTag() {
        imageConfig.nameAndTag
    }

    void hostname(String hostname) {
        option("--hostname=${hostname}")
    }

    void link(String linkedContainer) {
        dependsOn(linkedContainer.replaceFirst(":.*", ""))
        option("--link=${linkedContainer}")
    }

    void publish(String publishedPort) {
        option("--publish=${publishedPort}")
    }

    void volume(String volume) {
        option("--volume=${volume}")
    }

    void volumesFrom(String volumesFrom) {
        dependsOn(volumesFrom)
        option("--volumes-from=${volumesFrom}")
    }

    void env(String env) {
        option("--env=${env}")
    }

    void option(String option) {
        options.add(option)
    }

    void dependsOn(String containerName) {
        referencedContainerNames.add(containerName)
    }

    void commandWithArgs(String commandWithArgs) {
        this.commandWithArgs = commandWithArgs
    }

    void dataVolumeContainer() {
        dataVolumeContainer = true
    }

    List<String> getOptions() {
        ["--name=${name}", options].flatten()
    }

    List<String> getReferencedContainerNames() {
        referencedContainerNames.clone()
    }

    String createTaskName(DockerAction action) {
        "${action.displayName}${displayName.capitalize()}"
    }

    List<String> getCreateContainerArgs() {
        List<String> dockerArgs = []
        dockerArgs.addAll(getOptions())
        dockerArgs.add(imageNameAndTag)
        if (commandWithArgs) {
            dockerArgs.addAll(commandWithArgs.split(/\s+/))
        }
        dockerArgs
    }

    void validate() {
        if (!name) {
            throw new ValidationException("Missing required property, 'name'")
        }
    }

    ContainerConfig clone() {
        super.clone()
    }

}
