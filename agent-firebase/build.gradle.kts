description = "Firebase AI Logic model factory for the SunMobile AI Agent SDK."

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sunmobile.aiagent.firebase"
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
    api(project(":agent-adk"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.google.adk.kotlin.firebase.android)
}
