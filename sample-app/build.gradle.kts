plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.sunmobile.aiagent.sample"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = "com.sunmobile.aiagent.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}
dependencies {
    implementation(project(":agent-core"))
    implementation(libs.kotlinx.coroutines.android)
}
