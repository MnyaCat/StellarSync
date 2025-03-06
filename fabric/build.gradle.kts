import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    id("fabric-loom") version "1.9-SNAPSHOT"
    id("maven-publish")
}

val MINECRAFT_VERSION: String by rootProject.extra
val YARN_MAPPINGS: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val KOTLIN_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

version = MOD_VERSION
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("stellar_sync_fabric") {
            sourceSet("main")
            sourceSet("client")
        }
    }

    log4jConfigs.from(file("/home/mnyacat/IdeaProjects/StellarSync/fabric/src/main/resources/log4j.xml"))
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
    mavenCentral()
}

dependencies {

    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    mappings("net.fabricmc:yarn:${YARN_MAPPINGS}:v2")
    modImplementation("net.fabricmc:fabric-loader:${FABRIC_LOADER_VERSION}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${KOTLIN_LOADER_VERSION}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${FABRIC_API_VERSION}")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation(project(":common"))

}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", MINECRAFT_VERSION)
    inputs.property("loader_version", FABRIC_LOADER_VERSION)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to MINECRAFT_VERSION,
            "loader_version" to FABRIC_LOADER_VERSION,
            "kotlin_loader_version" to KOTLIN_LOADER_VERSION
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

/*tasks.shadowJar {
    // classifierを空文字にすると、出力jarの名前が通常のjarと同じになります
    archiveClassifier.set("")
    // runtimeClasspathに含まれるすべての依存関係を統合
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // 必要に応じて不要なメタ情報などを除外する
    // exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}*/

/*tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}*/

// configure the maven publication
/*publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}*/
