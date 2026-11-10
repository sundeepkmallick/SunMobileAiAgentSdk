description = "Google ADK Android runner and bridge for the SunMobile AI Agent SDK."

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sunmobile.aiagent.adk"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    defaultConfig { minSdk = 26 }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(project(":agent-core"))
    api("com.google.adk:google-adk-kotlin-core-android:1.0.0")
}
