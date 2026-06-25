import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("com.gradleup.shadow") version "8.3.0"
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    base
}

version = "${project.property("mod_version")}+${project.property("minecraft_version")}"
group = "com.presencecraft"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("presencecraft") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

val discordLib by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    implementation("net.java.dev.jna:jna:5.14.0")
    include("net.java.dev.jna:jna:5.14.0")

    modImplementation("me.shedaniel.cloth:cloth-config-fabric:15.0.140") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    modImplementation("com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}")

    val discordIpcFile = files("libs/discord-ipc-1.1.jar")
    
    implementation(discordIpcFile)
    
    discordLib(discordIpcFile)
}

tasks.processResources {
    val version = version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    val projectName = project.name
    archiveClassifier.set("thin")

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

tasks.shadowJar {
    configurations = listOf(discordLib)
    
    from(sourceSets.main.get().output)
    from(sourceSets.getByName("client").output)
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependencies {
        exclude(dependency("net.fabricmc:.*"))
        exclude(dependency("org.jetbrains.kotlin:.*"))
    }

    archiveClassifier.set("all-raw")
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    
    archiveClassifier.set("") 
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}