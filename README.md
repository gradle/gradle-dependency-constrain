# Gradle Dependency Constrain

This is a POC for constraining Gradle Dependencies with a simple `constraints.xml` file.

This aims to support the goals of the
[Supporting Automated Dependency Updating in the Gradle Ecosystem](https://docs.google.com/document/d/10OAehVIu3ehKvg60BMrMw5s3U7tTddeXIkyVw-KhFqU/edit#)
proposal.

## Implementation Details

This project is implemented as two components, a Gradle Plugin and a library. The library the logic for parsing
the `constraints.xml` file and the plugin is the Gradle Plugin that is used to apply the constraints. The idea behind
the library is to allow this code to be shared both between this plugin and the `gradle/gradle` build.
