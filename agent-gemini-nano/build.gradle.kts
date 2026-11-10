description = "Gemini Nano / ML Kit Agent API adapter for the SunMobile AI Agent SDK."

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sunmobile.aiagent.gemininano"
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
    implementation(libs.mlkit.genai.agent)
    implementation(libs.mlkit.genai.prompt)
}
