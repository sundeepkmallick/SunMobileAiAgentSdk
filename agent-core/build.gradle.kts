plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(11) }
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
