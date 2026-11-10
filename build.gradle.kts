import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    group = "com.sunmobile.aiagent"
    version = "0.1.0-rc.1"
}

val publishedSdkModules = listOf(
    ":agent-core",
    ":agent-adk",
    ":agent-firebase",
    ":agent-litertlm",
    ":agent-gemini-nano"
)

subprojects {
    if (path !in publishedSdkModules) return@subprojects

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    afterEvaluate {
        val androidLibrary = plugins.hasPlugin("com.android.library")

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = "com.sunmobile.aiagent"
                    artifactId = project.name
                    version = rootProject.version.toString()
                    from(components[if (androidLibrary) "release" else "java"])

                    pom {
                        name.set("SunMobile AI Agent SDK - ${project.name}")
                        description.set(project.description ?: "SunMobile AI Agent SDK module ${project.name}")
                        url.set("https://github.com/sundeepkmallick/SunMobileAiAgentSdk")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("sundeepkmallick")
                                name.set("Sundeep K. Mallick")
                                email.set("sundeepkmallick@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/sundeepkmallick/SunMobileAiAgentSdk.git")
                            developerConnection.set("scm:git:ssh://github.com/sundeepkmallick/SunMobileAiAgentSdk.git")
                            url.set("https://github.com/sundeepkmallick/SunMobileAiAgentSdk")
                        }
                    }
                }
            }
            // Deliberately local-only. No remote Maven repository is configured.
            repositories {
                mavenLocal()
            }
        }

        extensions.configure<SigningExtension> {
            val signingKey = providers.gradleProperty("signingKey").orNull
                ?: System.getenv("SIGNING_KEY")
            val signingPassword = providers.gradleProperty("signingPassword").orNull
                ?: System.getenv("SIGNING_PASSWORD")

            isRequired = !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()
            if (isRequired) {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

tasks.register("publishSdkToMavenLocal") {
    group = "publishing"
    description = "Publishes all five SDK modules to the local Maven repository only."
    dependsOn(publishedSdkModules.map { "$it:publishToMavenLocal" })
}
