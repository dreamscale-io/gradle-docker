package org.dreamscale.gradle.docker

import com.google.common.base.CaseFormat
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString(includePackage = false, includeFields = true)
@EqualsAndHashCode(includes = "imageName", includeFields = true)
class ImageConfig implements EntityConfig {

    static final String TYPE_NAME = "image"

    private ImageName imageName
    private ImageName.Factory imageNameFactory

    // for testing only
    private ImageConfig() {
        this(new ImageName.Factory())
    }

    ImageConfig(ImageName.Factory imageNameFactory) {
        this.imageNameFactory = imageNameFactory
    }

    void name(String name) {
        setName(name)
    }

    void setName(String name) {
        imageName = imageNameFactory.create(name)
    }

    String getName() {
        imageName?.qualifiedName
    }

    String getTag() {
        imageName?.tag
    }

    String getNameAndTag() {
        imageName?.qualifiedNameAndTag
    }

    String getShortName() {
        imageName?.repositoryName
    }

    String getDisplayName() {
        CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, shortName)
    }

    String getTypeName() {
        TYPE_NAME
    }

    String createTaskName(DockerAction action) {
        "${action.displayName}${displayName.capitalize()}"
    }

    void validate() {
        if (!name) {
            throw new ValidationException("Missing required property, 'name'")
        }
    }

}
