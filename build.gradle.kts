import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.16.0"
    id("org.jetbrains.intellij.platform.grammarkit") version "2.16.0"
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
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    // IntelliJ 2026.2 is compiled for Java 25, but the build toolchain is JDK 21
    // (Kotlin compilation still works because K2 reads the newer class files). The
    // Grammar-Kit generator is a JavaExec that loads platform classes, so it must
    // run on the JetBrains Runtime (Java 25) bundled with the resolved IDE.
    val platformPath = extensions.getByType<IntelliJPlatformExtension>().platformPath
    val jbrExecutable = platformPath.resolve("jbr/bin/java.exe").toFile().absolutePath

    withType<GenerateParserTask>().configureEach {
        sourceFile = file("src/main/kotlin/com/freemarkerplus/psi/Freemarker.bnf")
        targetRootOutputDir = file("src/main/gen")
        purgeOldFiles = false
        setExecutable(jbrExecutable)
    }

    withType<GenerateLexerTask>().configureEach {
        sourceFile = file("src/main/grammar/_FtlLexer.flex")
        targetRootOutputDir = file("src/main/gen")
        skeleton = file("src/main/grammar/idea-flex.skeleton")
        purgeOldFiles = false
        setExecutable(jbrExecutable)
    }

    // The generated Grammar-Kit parser (and FtlParserUtil) reference
    // com.intellij.lang.parser.GeneratedParserUtilBase, which lives in the IDE's
    // impl jars (e.g. intellij.platform.analysis.impl) rather than the public-API
    // compile classpath. Expose the full platform classpath to Java compilation.
    // Note: those impl jars are Java 25 class files, so compiling the generated
    // parser also requires a JDK 25 toolchain (see task report); `compileKotlin`
    // is unaffected and passes on the JDK 21 toolchain.
    withType<JavaCompile>().configureEach {
        classpath += configurations["intellijPlatformClasspath"]
    }
}
