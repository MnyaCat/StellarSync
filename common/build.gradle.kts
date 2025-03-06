plugins {
    kotlin("jvm")
}

val MOD_VERSION: String by rootProject.extra

group = "dev.mnyacat"
version = MOD_VERSION

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0")

    testImplementation(kotlin("test"))
}

tasks.build {
}

tasks.test {
    useJUnitPlatform()
}