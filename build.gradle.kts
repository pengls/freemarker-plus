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
version = "0.4.0"

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
        // Workaround for 2026.2 EAP test-mode module resolution (YouTrack IJPL-248701):
        // without the "misc" libraries plugin, `intellij.libraries.lucene.common` is
        // unresolvable in tests, which cascades into excluding our plugin (and thus its
        // `lang.parserDefinition`/`lang.fileViewProviderFactory` extensions) so
        // PsiFileFactory.createFileFromText returns null. Test-only; does not affect
        // the production plugin.
        testBundledPlugin("intellij.libraries.misc.plugin")
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    // Build JDK 25 (the bundled JetBrains Runtime), matching the 2026.2 platform's
    // Java 25 class files. See gradle.properties (org.gradle.java.installations.paths).
    jvmToolchain(25)
}

intellijPlatform {
    publishing {
        // JetBrains Marketplace permanent token. Never hard-code it here — set the
        // environment variable (e.g. $env:JETBRAINS_MARKETPLACE_TOKEN = "perm-...")
        // before running `gradle.bat publishPlugin`.
        token.set(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))
    }
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

    // The Grammar-Kit generator is a JavaExec that otherwise runs on the Gradle
    // daemon JVM (JDK 21), so point it explicitly at the bundled JetBrains Runtime
    // (Java 25) so it can load the 2026.2 platform classes.
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
    // compile classpath. Expose the full platform classpath to Java compilation
    // (compiled with the JDK 25 toolchain configured above).
    withType<JavaCompile>().configureEach {
        classpath += configurations["intellijPlatformClasspath"]
    }
}
