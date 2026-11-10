description = "LiteRT-LM model factory for the SunMobile AI Agent SDK."

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sunmobile.aiagent.litertlm"
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
    implementation(libs.google.adk.kotlin.litertlm.android)
    implementation(libs.google.ai.edge.litertlm.android)
}
