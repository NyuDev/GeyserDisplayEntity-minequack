plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "net.imaginefun"
version = "1.0.14"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

// Compiles against the Geyser standalone jar we run, which bundles the api and core
val geyserJar = providers.gradleProperty("geyserJar").orElse("${System.getProperty("user.home")}/Documents/code/imaginefun/resources/imaginefun/templates/geyser/geyser.jar")

dependencies {
    compileOnly(files(geyserJar))

    implementation("org.spongepowered:configurate-yaml:4.2.0-GeyserMC-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}-${version}.jar")

    relocate("org.spongepowered.configurate", "me.geyserextensionists.geyserdisplayentity.libs.configurate")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("extension.yml") {
        expand(props)
    }
    from("packs/GeyserDisplayEntityPack.mcpack")
}
