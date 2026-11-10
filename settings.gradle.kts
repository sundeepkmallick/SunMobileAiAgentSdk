pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SunMobileAiAgentSdk"
include(":agent-core", ":agent-adk", ":agent-firebase", ":agent-litertlm", ":agent-mlkit", ":agent-gemini-nano", ":sample-app")
