

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
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
    implementation("io.arrow-kt:arrow-core:0.11.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "14"
}

application {
    mainClassName = "memimg.MemoryImageKt"
}
