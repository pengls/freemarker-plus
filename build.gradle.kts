import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

fun stripCoroutinesAgent(providers: MutableList<CommandLineArgumentProvider>) {
    val stripped = providers.map { original ->
        object : CommandLineArgumentProvider {
            override fun asArguments(): Iterable<String> =
                original.asArguments().filterNot {
                    it.startsWith("-javaagent:") && it.contains("coroutines-javaagent")
                }
        }
    }
    providers.clear()
    providers.addAll(stripped)
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.9.0"
}

group = "com.freemarkerplus"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2")
        // testFramework(TestFrameworkType.Platform) — disabled: IPGP 2.9.0 can't parse IDEA 2026.2 module descriptors
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}

tasks.withType<JavaExec>().configureEach {
    stripCoroutinesAgent(jvmArgumentProviders)
}
tasks.withType<Test>().configureEach {
    stripCoroutinesAgent(jvmArgumentProviders)
}
