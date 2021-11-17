/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

fun Project.configureGradleDependencyConstrainProject() {
    description =
        "CI of the Gradle Dependency Constrain project (https://github.com/gradle/gradle-dependency-constrain)"

    params {
        java8Home(Os.linux)
    }

    val quickFeedbackBuildType = buildType("Quick Feedback") {
        steps {
            gradleCustom {
                tasks = "clean build"
            }
        }
    }

    val verifyAllBuildType = buildType("Verify all") {
        triggers.schedule {
            schedulingPolicy = daily {
                hour = 2
            }
            branchFilter = "+:refs/head/main"
            triggerBuild = always()
            withPendingChangesOnly = false
        }

        dependencies {
            snapshot(quickFeedbackBuildType) {
                onDependencyFailure = FailureAction.CANCEL
                onDependencyCancel = FailureAction.CANCEL
            }
        }
    }

    subProject("Release") {
        buildType("Development") {
            description =
                "Publishes Gradle test retry plugin to development plugin portal (plugins.grdev.net)"
            params {
                param("env.GRADLE_PUBLISH_KEY", "%development.plugin.portal.publish.key%")
                password(
                    "env.GRADLE_PUBLISH_SECRET",
                    "%development.plugin.portal.publish.secret%",
                    display = ParameterDisplay.NORMAL
                )
            }
            steps {
                gradleCustom {
                    tasks = "clean devSnapshot :plugin:publishPlugins -x test"
                    gradleParams += " -Dgradle.portal.url=https://plugins.grdev.net %pluginPortalPublishingFlags%"
                }
            }
            dependencies {
                snapshot(verifyAllBuildType) {
                    onDependencyFailure = FailureAction.CANCEL
                    onDependencyCancel = FailureAction.CANCEL
                }
            }
        }
    }
}
