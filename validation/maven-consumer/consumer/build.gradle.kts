plugins {
    id("com.android.library")
}

android {
    namespace = "com.sunmobile.validation.consumer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation("com.sunmobile.aiagent:agent-core:0.1.0-rc.1")
    implementation("com.sunmobile.aiagent:agent-adk:0.1.0-rc.1")
    implementation("com.sunmobile.aiagent:agent-firebase:0.1.0-rc.1")
    implementation("com.sunmobile.aiagent:agent-litertlm:0.1.0-rc.1")
    implementation("com.sunmobile.aiagent:agent-gemini-nano:0.1.0-rc.1")
}
