description = "Backend-neutral Kotlin/JVM agent contracts, sessions, tools and policy execution."

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
