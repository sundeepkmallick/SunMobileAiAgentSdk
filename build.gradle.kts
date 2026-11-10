import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    group = "io.github.sundeepkmallick"
    version = "0.1.0-rc.2"
}

val publishedSdkModules = listOf(
    ":agent-core",
    ":agent-adk",
    ":agent-firebase",
    ":agent-litertlm",
    ":agent-mlkit"
)

subprojects {
    if (path !in publishedSdkModules) return@subprojects
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
}

// AGP creates the release SoftwareComponent late in project configuration.
gradle.projectsEvaluated {
    publishedSdkModules.forEach { modulePath ->
        val module = project(modulePath)
        val componentName = if (module.plugins.hasPlugin("com.android.library")) "release" else "java"
        val component = module.components.findByName(componentName)
            ?: throw GradleException("Cannot publish $modulePath: component '$componentName' was not created.")

        module.extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = rootProject.group.toString()
                    artifactId = module.name
                    version = rootProject.version.toString()
                    from(component)
                    pom {
                        name.set("SunMobile AI Agent SDK - ${module.name}")
                        description.set(module.description ?: "SunMobile AI Agent SDK module ${module.name}")
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
            repositories { mavenLocal() }
        }

        module.extensions.configure<SigningExtension> {
            val key = module.providers.gradleProperty("signingKey").orNull ?: System.getenv("SIGNING_KEY")
            val password = module.providers.gradleProperty("signingPassword").orNull ?: System.getenv("SIGNING_PASSWORD")
            isRequired = !key.isNullOrBlank() && !password.isNullOrBlank()
            if (isRequired) {
                useInMemoryPgpKeys(key, password)
                sign(module.extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

tasks.register("publishSdkToMavenLocal") {
    group = "publishing"
    description = "Publishes all five SDK modules to the local Maven repository only."
    dependsOn(publishedSdkModules.map { "$it:publishToMavenLocal" })
}
