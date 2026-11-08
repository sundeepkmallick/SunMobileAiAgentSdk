plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.sunmobile.aiagent.gemininano"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    api(project(":agent-core"))
    implementation(libs.mlkit.genai.agent)
    implementation(libs.mlkit.genai.prompt)
}
