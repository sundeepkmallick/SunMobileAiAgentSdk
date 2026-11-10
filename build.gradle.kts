plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    group = "com.sunmobile.aiagent"
    version = "0.1.0-rc.1"
}
