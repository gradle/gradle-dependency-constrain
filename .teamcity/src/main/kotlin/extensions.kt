import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.toId

fun BuildType.agentRequirement(os: Os) {
    requirements {
        contains("teamcity.agent.jvm.os.name", os.requirementName)
    }
}

fun ParametrizedWithType.java8Home(os: Os) {
    param("env.JAVA_HOME", "%${os.name}.java8.oracle.64bit%")
}

const val useGradleInternalScansServer = "-I gradle/init-scripts/build-scan.init.gradle.kts"

const val buildCacheSetup = "--build-cache"

/**
 * Creates a new subproject with the given name, automatically deriving the [Project.id] from the name.
 *
 * Using this method also implicitly ensures that subprojects are ordered by creation order.
 */
fun Project.subProject(projectName: String, init: Project.() -> Unit): Project {
    val parent = this
    val subProject = subProject {
        name = projectName
        id = RelativeId(name.toId(stripRootProject(parent.id.toString())))
    }.apply(init)

    this.subProjectsOrder += subProject.id!!

    return subProject
}

fun Project.buildType(buildTypeName: String, init: BuildType.() -> Unit): BuildType {
    val buildType = buildType {
        name = buildTypeName
        id = RelativeId(name.toId(stripRootProject(this@buildType.id.toString())))

        artifactRules = "build/reports/** => reports"
        agentRequirement(Os.linux) // default

        commitStatus()

        params {
            java8Home(Os.linux)

            param("env.GRADLE_CACHE_REMOTE_URL", "%gradle.cache.remote.url%")
            param("env.GRADLE_CACHE_REMOTE_USERNAME", "%gradle.cache.remote.username%")
            param("env.GRADLE_CACHE_REMOTE_PASSWORD", "%gradle.cache.remote.password%")
        }

        vcs {
            root(DslContext.settingsRoot)
            checkoutMode = CheckoutMode.ON_AGENT
        }
    }.apply(init)

    this.buildTypesOrderIds += buildType.id!!
    return buildType
}

private fun stripRootProject(id: String): String {
    return id.replace("${DslContext.projectId.value}_", "")
}

fun BuildType.commitStatus() {
    features {
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%githubTeamcityBotApiKey%"
                }
            }
        }
    }
}

fun BuildSteps.gradleCustom(init: GradleBuildStep.() -> Unit) {
    gradle {
        buildFile = ""
        gradleParams = "-s $buildCacheSetup $useGradleInternalScansServer"
        init()
    }
}
