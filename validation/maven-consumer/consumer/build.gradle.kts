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
    implementation("io.github.sundeepkmallick:agent-core:0.1.0-rc.1")
    implementation("io.github.sundeepkmallick:agent-adk:0.1.0-rc.1")
    implementation("io.github.sundeepkmallick:agent-firebase:0.1.0-rc.1")
    implementation("io.github.sundeepkmallick:agent-litertlm:0.1.0-rc.1")
    implementation("io.github.sundeepkmallick:agent-gemini-nano:0.1.0-rc.1")
}
