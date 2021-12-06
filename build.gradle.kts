import net.blueberrymc.blueberryFarm.blueberry

plugins {
    java
    id("net.blueberrymc.blueberryFarm") version("1.0.3-SNAPSHOT") // https://github.com/BlueberryMC/BlueberryFarm
}

group = "xyz.acrylicstyle.multiVersion"
version = "0.0.2"

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

blueberry {
    minecraftVersion.set("1.18")
    apiVersion.set("0.1.0")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo2.acrylicstyle.xyz/") }
    maven { url = uri("https://repo.spongepowered.org/maven") }
}

dependencies {
    blueberry()
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
