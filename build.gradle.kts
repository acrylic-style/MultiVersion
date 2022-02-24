import net.blueberrymc.blueberryFarm.blueberry

plugins {
    java
    id("net.blueberrymc.blueberryFarm") version("1.0.4-SNAPSHOT") // https://github.com/BlueberryMC/BlueberryFarm
}

group = "xyz.acrylicstyle.multiVersion"
version = "1.0.2"

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

blueberry {
    minecraftVersion.set("22w06a")
    apiVersion.set("1.0.0-SNAPSHOT")
}

repositories {
    // mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.blueberrymc.net/repository/maven-public/") }
}

dependencies {
    blueberry()
    implementation("com.mojang:logging:1.0.0")
}

tasks {
    withType<net.blueberrymc.blueberryFarm.tasks.RunClient> {
        this.addArgs("--mixin mixins.multiversion.json")
    }

    withType<net.blueberrymc.blueberryFarm.tasks.RunServer> {
        this.addArgs("--mixin mixins.multiversion.json")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Jar> {
        manifest.attributes(
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "MixinConfigs" to "mixins.multiversion.json",
        )
    }
}
