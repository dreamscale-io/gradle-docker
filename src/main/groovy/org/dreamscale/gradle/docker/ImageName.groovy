package org.dreamscale.gradle.docker

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.api.GradleException

@ToString(includePackage = false, includeFields = true)
@EqualsAndHashCode(includeFields = true)
class ImageName {

    private String registryHost
    private String registryUsername
    private String repositoryName
    private String tag

    String getQualifiedName() {
        String qualifiedName = ""
        if (registryHost) {
            qualifiedName += "${registryHost}/"
        }
        if (registryUsername) {
            qualifiedName += "${registryUsername}/"
        }
        qualifiedName + repositoryName
    }

    String getQualifiedNameAndTag() {
        "${getQualifiedName()}:${tag}"
    }

    String getRepositoryName() {
        repositoryName
    }

    String getTag() {
        tag
    }


    static class Factory {

        String defaultRegistryHost
        String defaultRegistryUsername
        String defaultTag = "latest"

        ImageName create(String name) {
            ImageName imageName = new ImageName()
            imageName.registryHost = defaultRegistryHost
            imageName.registryUsername = defaultRegistryUsername

            String[] segments = name.split("/")
            switch (segments.length) {
                case 1:
                    imageName.repositoryName = segments[0]
                    break
                case 2:
                    imageName.registryUsername = segments[0]
                    imageName.repositoryName = segments[1]
                    break
                case 3:
                    imageName.registryHost = segments[0]
                    imageName.registryUsername = segments[1]
                    imageName.repositoryName = segments[2]
                    break
                default:
                    throw new GradleException("Invalid image name '${name}', must correspond to pattern [REGISTRYHOST/][USERNAME/]NAME[:TAG]")
            }

            def tagMatcher = imageName.repositoryName =~ /(\S+):(\S+)$/
            if (tagMatcher) {
                imageName.repositoryName = tagMatcher.group(1)
                imageName.tag = tagMatcher.group(2)
            } else {
                imageName.tag = defaultTag
            }
            imageName
        }

    }

}
