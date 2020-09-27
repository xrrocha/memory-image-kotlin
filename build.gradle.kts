

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("io.arrow-kt:arrow-core:0.11.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC2")

    implementation("io.github.microutils:kotlin-logging:1.12.0")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    implementation(kotlin("script-runtime"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "14"
}

application {
    mainClassName = "memimg.MemoryImageKt"
}
