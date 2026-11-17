plugins {
    kotlin("jvm") version "2.1.0"
    id("fabric-loom") version "1.9-SNAPSHOT" apply false
}

group = "dev.mnyacat"
version = "1.0-SNAPSHOT"

val MINECRAFT_VERSION by extra { "1.21.7" }
val YARN_MAPPINGS by extra { "1.21.7+build.8" }
val FABRIC_LOADER_VERSION by extra { "0.16.14" }
val KOTLIN_LOADER_VERSION by extra { "1.13.4+kotlin.2.2.0" }
val FABRIC_API_VERSION by extra { "0.129.0+1.21.7" }
val MOD_VERSION by extra { "1.1.0" }

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
