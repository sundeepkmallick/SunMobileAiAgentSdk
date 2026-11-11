description = "Gemini Nano backend using Google ML Kit GenAI Agent and Prompt APIs."

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sunmobile.aiagent.mlkit"
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
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
