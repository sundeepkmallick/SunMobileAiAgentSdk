plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.sunmobile.aiagent.firebase"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    api(project(":agent-adk"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.google.adk.kotlin.firebase.android)
}
