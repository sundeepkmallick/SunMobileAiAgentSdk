pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SunMobileAiAgentSdk"
include(":agent-core", ":agent-adk", ":agent-firebase", ":agent-litertlm", ":agent-gemini-nano", ":sample-app")
