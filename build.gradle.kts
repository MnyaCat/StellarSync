plugins {
    kotlin("jvm") version "2.1.0"
    id("fabric-loom") version "1.9-SNAPSHOT" apply false
}

group = "dev.mnyacat"
version = "1.0-SNAPSHOT"

val MINECRAFT_VERSION by extra { "1.21.4" }
val YARN_MAPPINGS by extra { "1.21.4+build.8" }
val FABRIC_LOADER_VERSION by extra { "0.16.10" }
val KOTLIN_LOADER_VERSION by extra { "1.13.1+kotlin.2.1.10" }
val FABRIC_API_VERSION by extra { "0.115.1+1.21.4" }
val MOD_VERSION by extra { "1.0-SNAPSHOT" }

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    dependencies {
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}