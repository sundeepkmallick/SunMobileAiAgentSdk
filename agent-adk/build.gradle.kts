plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.sunmobile.aiagent.adk"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    api(project(":agent-core"))
    api("com.google.adk:google-adk-kotlin-core-android:1.0.0")
}
