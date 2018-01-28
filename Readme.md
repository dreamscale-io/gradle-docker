[![Build Status](https://travis-ci.org/dreamscale-io/gradle-docker.svg?branch=master)](https://travis-ci.org/dreamscale-io/gradle-docker)

# Gradle Docker 

Gradle plugin to simplify docker lifecycle tasks.

# Publishing to Bintray

Make sure you have a bintray account and are a member of the [DreamScale organization](https://bintray.com/dreamscale/organization/edit)

Open your [user profile](https://bintray.com/profile/edit/organizations) and retrieve your API Key

Execute bintray upload `gw bintrayUpload -Pbintray.user=<bintray user> -Pbintray.apiKey=<api key>`

Open the DreamScale [gradle-docker](https://bintray.com/dreamscale/maven-public/org.dreamscale%3Agradle-docker) package and
click the [Publish](https://bintray.com/dreamscale/maven-public/org.dreamscale%3Agradle-docker/publish) link
