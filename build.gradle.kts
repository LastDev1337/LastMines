plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

group = "ru.last.mines"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.by1337.space/repository/maven-releases/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.fancyinnovations.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("dev.by1337.core:BLibV2:1.7.3") // BLibV2
    compileOnly("me.clip:placeholderapi:2.12.2") // PlaceholderAPI
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.5") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.5") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.5") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.6") // DecentHolograms
    compileOnly("dev.by1337.bmenu:BMenu:2.3") // BMenu
    compileOnly("de.oliver:FancyHolograms:2.4.0") // FancyHolograms
    implementation("org.bstats:bstats-bukkit:3.2.1") // bStats
}

tasks {
    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.bstats", "ru.last.mines.bstats")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name.lowercase()
            version = project.version.toString()

            artifact(tasks.named("shadowJar"))
        }
    }
    repositories {
        maven {
            url = uri("https://repo.laststudio.space/releases")
            credentials {
                username = project.findProperty("repoUser") as String?
                password = project.findProperty("repoPass") as String?
            }
        }
    }
}
