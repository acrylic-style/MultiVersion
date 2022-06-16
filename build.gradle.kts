import net.blueberrymc.blueberryFarm.blueberry

plugins {
    java
    id("net.blueberrymc.blueberryFarm") version("1.0.4-SNAPSHOT") // https://github.com/BlueberryMC/BlueberryFarm
}

group = "xyz.acrylicstyle.multiVersion"
version = "1.0.5"

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

blueberry {
    minecraftVersion.set("1.19")
    apiVersion.set("1.5.0-SNAPSHOT")
}

repositories {
    // mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.blueberrymc.net/repository/maven-public/") }
}

dependencies {
    implementation("com.mojang:logging:1.0.0")
    blueberry()
}

tasks {
    withType<net.blueberrymc.blueberryFarm.tasks.RunClient> {
        this.addArgs("--debug --mixin mixins.multiversion.json")
        this.environment("LOG4J_CONFIGURATION_FILE", "log4j2-debug.xml")
    }

    withType<net.blueberrymc.blueberryFarm.tasks.RunServer> {
        this.addArgs("--mixin mixins.multiversion.json")
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        manifest.attributes(
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "MixinConfigs" to "mixins.multiversion.json",
        )
    }
}
