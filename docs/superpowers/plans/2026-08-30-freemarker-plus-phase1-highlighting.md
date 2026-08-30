# Freemarker Plus — Phase 1 语法高亮 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个 IntelliJ 插件，为 `.ftl` 文件提供 Freemarker + HTML + CSS + JS 语法高亮。

**Architecture:** Freemarker 作为模板语言（`TemplateLanguage`），手写词法分析器切分「Freemarker 区域 vs 数据区域」，高亮器用 `LayeredLexer` 把平台内置的 HTML/CSS/JS 高亮器分层叠加到数据区域上，Freemarker token 用自定义颜色键着色。

**Tech Stack:** Kotlin、Gradle Kotlin DSL、IntelliJ Platform Gradle Plugin 2.x、JUnit4 + IntelliJ 测试框架。

**Spec:** `docs/superpowers/specs/2026-08-30-freemarker-plus-syntax-highlighting-design.md`

## Global Constraints

- 平台版本：`2026.2`（build `262`）；`sinceBuild = 262`，`untilBuild = 262.*`
- 语言 ID：`FTL`；文件类型名：`FreeMarker Template`；扩展名：`ftl`
- 源码语言：Kotlin；构建脚本：Kotlin DSL；基础包名：`com.freemarkerplus`
- 颜色一律走 `TextAttributesKey` + `DefaultLanguageHighlighterColors`（主题感知），**禁止硬编码 RGB**
- 工具链：JDK 21（toolchain）、Gradle 9.7.1、IntelliJ Platform Gradle Plugin `2.16.0`、Kotlin `2.3.0`
- 每个 Task 结束都必须 `git commit`；提交信息用 `feat:` / `test:` / `chore:` 前缀
- 本阶段**不写 PSI 解析器**，因此无补全/跳转/折叠/语义报错

---

## File Structure

```
freemarker-plus/
├── build.gradle.kts                                    # 构建脚本（IPGP 2.x DSL）
├── settings.gradle.kts                                 # 仓库/插件管理
├── gradle.properties                                   # Gradle JVM 参数
├── gradle/wrapper/gradle-wrapper.properties            # Gradle 8.14 包装器
├── .gitignore
├── src/main/kotlin/com/freemarkerplus/
│   ├── lang/FreemarkerLanguage.kt                      # 语言定义（TemplateLanguage 标记）
│   ├── lang/FreemarkerFileType.kt                      # .ftl 文件类型（LanguageFileType）
│   ├── lexer/FreemarkerTokenType.kt                    # Freemarker token 类型定义
│   ├── lexer/FreemarkerLexer.kt                        # 手写有状态词法分析器（核心）
│   ├── highlighting/FreemarkerColors.kt                # TextAttributesKey 定义 + token→key 映射
│   ├── highlighting/FreemarkerSyntaxHighlighter.kt     # LayeredLexer 高亮器
│   ├── highlighting/FreemarkerSyntaxHighlighterFactory.kt
│   └── highlighting/FreemarkerColorSettingsPage.kt     # 配色设置页
├── src/main/resources/META-INF/plugin.xml              # 插件描述 + 扩展点
└── src/test/kotlin/com/freemarkerplus/
    ├── lexer/FreemarkerLexerTest.kt
    └── highlighting/FreemarkerSyntaxHighlighterTest.kt
```

每个文件职责单一：`lang/` 定义语言与文件类型，`lexer/` 只做词法分析，`highlighting/` 只做 token→颜色映射与叠加。

---

## Task 1: 项目骨架 + 构建冒烟

**目标：** 建立可编译、能 `runIde` 启动 IDE 的最小插件工程（尚无高亮功能）。

**Files:**
- Create: `build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`、`.gitignore`、`gradle/wrapper/gradle-wrapper.properties`
- Create: `src/main/resources/META-INF/plugin.xml`（最小版，无扩展点）

**Interfaces:**
- Produces: 可被 `./gradlew buildPlugin` 打包、`./gradlew runIde` 启动的空插件工程。

- [ ] **Step 1: 写 `settings.gradle.kts`**

```kotlin
rootProject.name = "freemarker-plus"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

- [ ] **Step 2: 写 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
kotlin.code.style=official
```

- [ ] **Step 3: 写 `.gitignore`**

```gitignore
.gradle/
build/
.idea/
*.iml
out/
local.properties
```

- [ ] **Step 4: 写 `build.gradle.kts`**

```kotlin
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
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
        intellijIdeaCommunity("2026.2")
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(21)
}
```

- [ ] **Step 5: 写最小 `plugin.xml`**

```xml
<idea-plugin>
    <id>com.freemarkerplus</id>
    <name>Freemarker Plus</name>
    <vendor>freemarker-plus</vendor>
    <description><![CDATA[
        Syntax highlighting for FreeMarker templates (.ftl) with HTML/CSS/JS support.
    ]]></description>

    <idea-version since-build="262" until-build="262.*"/>

    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.lang</depends>
</idea-plugin>
```

- [ ] **Step 6: 生成 Gradle 包装器并验证构建**

在项目根目录（用 IDEA 2026.2.1 内置 Gradle，或本机 Gradle）执行：

```bash
gradle wrapper --gradle-version 8.14
./gradlew buildPlugin
```

预期：`BUILD SUCCESSFUL`，产出 `build/distributions/freemarker-plus-0.1.0.zip`。
若报 JDK 版本不匹配（Gradle 8.14 不支持 Java 25 运行 Gradle 本体），用 IDEA 安装一个 JDK 21：`Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM` 选择 JDK 21（IDEA 可 `Project Structure → SDKs → + → Download JDK` 下载 Temurin 21）。

- [ ] **Step 7: 冒烟启动 IDE**

```bash
./gradlew runIde
```

预期：启动一个含本插件的 IntelliJ IDEA 2026.2 沙箱实例，无启动报错，插件出现在 `Settings → Plugins → Installed`。

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "chore: scaffold IntelliJ plugin project (Kotlin + IPGP 2.x)"
```

---

## Task 2: Freemarker 语言、文件类型与词法分析器

**目标：** 定义语言/文件类型，TDD 实现核心 `FreemarkerLexer`（切分 Freemarker 构造与数据区域）。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/lang/FreemarkerLanguage.kt`
- Create: `src/main/kotlin/com/freemarkerplus/lang/FreemarkerFileType.kt`
- Create: `src/main/kotlin/com/freemarkerplus/lexer/FreemarkerTokenType.kt`
- Create: `src/main/kotlin/com/freemarkerplus/lexer/FreemarkerLexer.kt`
- Test: `src/test/kotlin/com/freemarkerplus/lexer/FreemarkerLexerTest.kt`

**Interfaces:**
- Produces:
  - `FreemarkerLanguage.INSTANCE`（`Language` + `TemplateLanguage`）
  - `FreemarkerFileType.INSTANCE`（`LanguageFileType`）
  - `FreemarkerTokenTypes.*`（`IElementType` 常量）
  - `FreemarkerLexer`（实现 `com.intellij.lexer.Lexer`，输出 `FreemarkerTokenTypes.*`，含数据区 `TEMPLATE_DATA`）

- [ ] **Step 1: 写语言定义**

`src/main/kotlin/com/freemarkerplus/lang/FreemarkerLanguage.kt`：

```kotlin
package com.freemarkerplus.lang

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

class FreemarkerLanguage private constructor() : Language("FTL"), TemplateLanguage {
    companion object {
        @JvmField
        val INSTANCE = FreemarkerLanguage()
    }
}
```

- [ ] **Step 2: 写文件类型**

`src/main/kotlin/com/freemarkerplus/lang/FreemarkerFileType.kt`：

```kotlin
package com.freemarkerplus.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object FreemarkerFileType : LanguageFileType(FreemarkerLanguage.INSTANCE) {
    override fun getName(): String = "FreeMarker Template"
    override fun getDescription(): String = "FreeMarker template file"
    override fun getDefaultExtension(): String = "ftl"
    override fun getIcon(): Icon = AllIcons.FileTypes.Html
}
```

> 若 `AllIcons.FileTypes.Html` 在当前 SDK 不存在，改用 `AllIcons.FileTypes.Text`。

- [ ] **Step 3: 写 token 类型**

`src/main/kotlin/com/freemarkerplus/lexer/FreemarkerTokenType.kt`：

```kotlin
package com.freemarkerplus.lexer

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

class FreemarkerTokenType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FreemarkerTokenType.$debugName"
}

object FreemarkerTokenTypes {
    @JvmField val COMMENT = FreemarkerTokenType("COMMENT")
    @JvmField val STRING = FreemarkerTokenType("STRING")
    @JvmField val KEYWORD = FreemarkerTokenType("KEYWORD")
    @JvmField val DIRECTIVE_NAME = FreemarkerTokenType("DIRECTIVE_NAME")
    @JvmField val INTERPOLATION = FreemarkerTokenType("INTERPOLATION")
    @JvmField val IDENTIFIER = FreemarkerTokenType("IDENTIFIER")
    @JvmField val NUMBER = FreemarkerTokenType("NUMBER")
    @JvmField val OPERATOR = FreemarkerTokenType("OPERATOR")
    @JvmField val BAD_CHARACTER = FreemarkerTokenType("BAD_CHARACTER")
}
```

- [ ] **Step 4: 写词法分析器测试（先失败）**

`src/test/kotlin/com/freemarkerplus/lexer/FreemarkerLexerTest.kt`：

```kotlin
package com.freemarkerplus.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FreemarkerLexerTest : BasePlatformTestCase() {

    private fun lex(text: String): List<Triple<IElementType?, Int, Int>> {
        val lexer = FreemarkerLexer()
        lexer.start(text, 0, text.length)
        val result = mutableListOf<Triple<IElementType?, Int, Int>>()
        while (lexer.tokenType != null) {
            result.add(Triple(lexer.tokenType, lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    private fun types(text: String): List<IElementType?> = lex(text).map { it.first }

    fun testInterpolation() {
        val text = "\${user.name}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.OPERATOR,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testString() {
        val text = "\${'hello'}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.STRING,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testDirective() {
        val text = "<#if x>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testClosingDirective() {
        val text = "</#if>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testComment() {
        val text = "<#-- hello -->"
        assertEquals(listOf(FreemarkerTokenTypes.COMMENT), types(text))
    }

    fun testEscapedInterpolationIsData() {
        val text = "\\\${x}"
        assertEquals(listOf(FreemarkerTokenTypes.TEMPLATE_DATA), types(text))
    }

    fun testPlainHtmlIsData() {
        val text = "<div class=\"a\">text</div>"
        assertEquals(listOf(FreemarkerTokenTypes.TEMPLATE_DATA), types(text))
    }
}
```

- [ ] **Step 5: 运行测试确认失败**

```bash
./gradlew test --tests "com.freemarkerplus.lexer.FreemarkerLexerTest"
```

预期：编译失败（`FreemarkerLexer` 尚未创建）。

- [ ] **Step 6: 实现 `FreemarkerLexer`**

`src/main/kotlin/com/freemarkerplus/lexer/FreemarkerLexer.kt`：

```kotlin
package com.freemarkerplus.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class FreemarkerLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var pos = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    private enum class Mode { DATA, INTERPOLATION, TAG }

    private var mode = Mode.DATA
    private var expectName = false

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.pos = startOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        this.mode = Mode.DATA
        this.expectName = false
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferEnd(): Int = endOffset

    override fun getBufferSequence(): CharSequence = buffer

    override fun advance() {
        tokenStart = pos
        if (pos >= endOffset) {
            tokenType = null
            tokenEnd = pos
            return
        }
        when (mode) {
            Mode.DATA -> advanceData()
            Mode.INTERPOLATION -> advanceInterpolation()
            Mode.TAG -> advanceTag()
        }
    }

    private fun advanceData() {
        when {
            startsWith(pos, "<#--") -> {
                tokenType = FreemarkerTokenTypes.COMMENT
                val end = indexOf(pos, "-->")
                pos = if (end >= 0) end + 3 else endOffset
                tokenEnd = pos
            }
            startsWith(pos, "</#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 3
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<@") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "\${") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.INTERPOLATION
            }
            else -> lexPlainData()
        }
    }

    private fun lexPlainData() {
        while (pos < endOffset) {
            val c = buffer[pos]
            if (c == '\\' && pos + 1 < endOffset && (buffer[pos + 1] == '$' || buffer[pos + 1] == '<')) {
                pos += 2
                continue
            }
            if (isFreemarkerStart(pos)) break
            pos++
        }
        tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
        tokenEnd = pos
    }

    private fun advanceInterpolation() {
        if (buffer[pos] == '}') {
            tokenType = FreemarkerTokenTypes.INTERPOLATION
            pos++
            tokenEnd = pos
            mode = Mode.DATA
            return
        }
        lexExpressionToken()
    }

    private fun advanceTag() {
        if (buffer[pos] == '>') {
            tokenType = FreemarkerTokenTypes.INTERPOLATION
            pos++
            tokenEnd = pos
            mode = Mode.DATA
            return
        }
        if (expectName) {
            if (buffer[pos].isWhitespace()) {
                lexWhitespace()
                return
            }
            if (buffer[pos].isJavaIdentifierStart()) {
                while (pos < endOffset && buffer[pos].isJavaIdentifierPart()) pos++
                tokenType = FreemarkerTokenTypes.DIRECTIVE_NAME
                tokenEnd = pos
                expectName = false
                return
            }
            pos++
            tokenType = FreemarkerTokenTypes.BAD_CHARACTER
            tokenEnd = pos
            return
        }
        lexExpressionToken()
    }

    private fun lexExpressionToken() {
        tokenStart = pos
        val c = buffer[pos]
        when {
            c.isWhitespace() -> lexWhitespace()
            c == '"' || c == '\'' -> lexString(c)
            c.isDigit() -> lexNumber()
            c.isJavaIdentifierStart() -> lexIdentifier()
            else -> {
                pos++
                tokenType = FreemarkerTokenTypes.OPERATOR
                tokenEnd = pos
            }
        }
    }

    private fun lexWhitespace() {
        while (pos < endOffset && buffer[pos].isWhitespace()) pos++
        tokenType = TokenType.WHITE_SPACE
        tokenEnd = pos
    }

    private fun lexString(quote: Char) {
        pos++
        while (pos < endOffset) {
            if (buffer[pos] == '\\' && pos + 1 < endOffset) {
                pos += 2
                continue
            }
            if (buffer[pos] == quote) {
                pos++
                break
            }
            pos++
        }
        tokenType = FreemarkerTokenTypes.STRING
        tokenEnd = pos
    }

    private fun lexNumber() {
        while (pos < endOffset && buffer[pos].isDigit()) pos++
        if (pos + 1 < endOffset && buffer[pos] == '.' && buffer[pos + 1].isDigit()) {
            pos++
            while (pos < endOffset && buffer[pos].isDigit()) pos++
        }
        tokenType = FreemarkerTokenTypes.NUMBER
        tokenEnd = pos
    }

    private fun lexIdentifier() {
        while (pos < endOffset && buffer[pos].isJavaIdentifierPart()) pos++
        val text = buffer.subSequence(tokenStart, pos).toString()
        tokenType = if (text in KEYWORDS) FreemarkerTokenTypes.KEYWORD else FreemarkerTokenTypes.IDENTIFIER
        tokenEnd = pos
    }

    private fun isFreemarkerStart(offset: Int): Boolean =
        startsWith(offset, "\${") || startsWith(offset, "<#") || startsWith(offset, "<@")

    private fun startsWith(offset: Int, text: String): Boolean {
        if (offset + text.length > endOffset) return false
        for (i in text.indices) {
            if (buffer[offset + i] != text[i]) return false
        }
        return true
    }

    private fun indexOf(from: Int, text: String): Int {
        if (from >= endOffset) return -1
        var i = from
        while (i <= endOffset - text.length) {
            if (startsWith(i, text)) return i
            i++
        }
        return -1
    }

    companion object {
        private val KEYWORDS = setOf(
            "true", "false", "and", "or", "not", "gt", "gte", "lt", "lte",
            "if", "else", "elseif", "list", "break", "continue",
            "include", "import", "assign", "local", "global",
            "macro", "function", "return", "switch", "case", "default",
            "as", "in", "using", "new"
        )
    }
}
```

- [ ] **Step 7: 运行测试确认通过**

```bash
./gradlew test --tests "com.freemarkerplus.lexer.FreemarkerLexerTest"
```

预期：`BUILD SUCCESSFUL`，7 个测试全部通过。

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat: add Freemarker language, file type, and lexer"
```

---

## Task 3: 语法高亮器（Freemarker + HTML）与工厂注册

**目标：** 把 Freemarker token 映射到颜色键，用 `LayeredLexer` 叠加 HTML 高亮器，注册到扩展点，使 `.ftl` 中 Freemarker 与 HTML 均高亮。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerColors.kt`
- Create: `src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighter.kt`
- Create: `src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighterFactory.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`（加 `fileType`、`lang.syntaxHighlighterFactory` 扩展点）
- Test: `src/test/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighterTest.kt`

**Interfaces:**
- Consumes: `FreemarkerTokenTypes.*`、`FreemarkerLexer`、`FreemarkerLanguage.INSTANCE`、`FreemarkerFileType.INSTANCE`
- Produces: `FreemarkerColors.*`（`TextAttributesKey`）、`FreemarkerSyntaxHighlighter`、`FreemarkerSyntaxHighlighterFactory`

- [ ] **Step 1: 写颜色键与映射**

`src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerColors.kt`：

```kotlin
package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerTokenTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.tree.IElementType

object FreemarkerColors {
    @JvmField val COMMENT = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    @JvmField val STRING = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_STRING", DefaultLanguageHighlighterColors.STRING)
    @JvmField val KEYWORD = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val DIRECTIVE_NAME = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_DIRECTIVE_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
    @JvmField val INTERPOLATION = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_INTERPOLATION", DefaultLanguageHighlighterColors.BRACES)
    @JvmField val NUMBER = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    @JvmField val OPERATOR = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    @JvmField val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_BAD_CHARACTER", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)

    fun keyFor(tokenType: IElementType): TextAttributesKey? = when (tokenType) {
        FreemarkerTokenTypes.COMMENT -> COMMENT
        FreemarkerTokenTypes.STRING -> STRING
        FreemarkerTokenTypes.KEYWORD -> KEYWORD
        FreemarkerTokenTypes.DIRECTIVE_NAME -> DIRECTIVE_NAME
        FreemarkerTokenTypes.INTERPOLATION -> INTERPOLATION
        FreemarkerTokenTypes.NUMBER -> NUMBER
        FreemarkerTokenTypes.OPERATOR -> OPERATOR
        FreemarkerTokenTypes.BAD_CHARACTER -> BAD_CHARACTER
        else -> null
    }
}
```

- [ ] **Step 2: 写高亮器**

`src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighter.kt`：

```kotlin
package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerLexer
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.freemarkerplus.lexer.FreemarkerTokenTypes

class FreemarkerSyntaxHighlighter(
    private val project: Project?,
    private val virtualFile: VirtualFile?
) : SyntaxHighlighterBase() {

    private val htmlHighlighter: SyntaxHighlighter? by lazy {
        SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, project, virtualFile)
    }

    override fun getHighlightingLexer(): Lexer {
        val layered = LayeredLexer(FreemarkerLexer())
        htmlHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.TEMPLATE_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        return layered
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        FreemarkerColors.keyFor(tokenType)?.let { return pack(it) }
        htmlHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        return TextAttributesKey.EMPTY_ARRAY
    }
}
```

- [ ] **Step 3: 写工厂**

`src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighterFactory.kt`：

```kotlin
package com.freemarkerplus.highlighting

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FreemarkerSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return FreemarkerSyntaxHighlighter(project, virtualFile)
    }
}
```

- [ ] **Step 4: 注册扩展点（修改 `plugin.xml`）**

在 `</idea-plugin>` 之前、`<depends>` 之后插入：

```xml
    <extensions defaultExtensionNs="com.intellij">
        <fileType name="FreeMarker Template"
                  implementationClass="com.freemarkerplus.lang.FreemarkerFileType"
                  fieldName="INSTANCE"
                  language="FTL"
                  extensions="ftl"/>
        <lang.syntaxHighlighterFactory language="FTL"
                                       implementationClass="com.freemarkerplus.highlighting.FreemarkerSyntaxHighlighterFactory"/>
    </extensions>
```

- [ ] **Step 5: 写高亮器映射测试**

`src/test/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighterTest.kt`：

```kotlin
package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerTokenTypes
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FreemarkerSyntaxHighlighterTest : BasePlatformTestCase() {

    fun testCommentMapsToCommentKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.COMMENT)
                .contains(FreemarkerColors.COMMENT)
        )
    }

    fun testStringMapsToStringKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.STRING)
                .contains(FreemarkerColors.STRING)
        )
    }

    fun testDirectiveNameMapsToDirectiveKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.DIRECTIVE_NAME)
                .contains(FreemarkerColors.DIRECTIVE_NAME)
        )
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
./gradlew test
```

预期：`BUILD SUCCESSFUL`（Task 2 与 Task 3 全部测试通过）。

- [ ] **Step 7: 手动验证 Freemarker + HTML 高亮**

创建临时文件 `sample.ftl`：

```html
<#-- header comment -->
<#if user.name gt "abc">
  <div class="card" id="main">
    <h1>Hello ${user.name}</h1>
  </div>
</#if>
```

```bash
./gradlew runIde
```

在沙箱 IDE 中打开 `sample.ftl`，预期：
- `<#-- header comment -->` 灰色
- `user.name`、`"abc"` 中字符串绿色
- `<#if>`、`</#if>` 指令名着色，`${` `}` 定界符着色
- `<div>`、`<h1>` HTML 标签黄色（平台 HTML 配色）
- `class="card"` 属性值绿色（平台 HTML 配色）

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat: add syntax highlighter with HTML overlay and register file type"
```

---

## Task 4: CSS / JS 嵌入高亮

**目标：** 让 `.ftl` 中 `<style>` / `<script>` 块内的 CSS / JS 获得完整高亮。

**Files:**
- Modify: `src/main/kotlin/com/freemarkerplus/lexer/FreemarkerTokenType.kt`（加 `STYLE_DATA`、`SCRIPT_DATA`）
- Modify: `src/main/kotlin/com/freemarkerplus/lexer/FreemarkerLexer.kt`（识别 `<style>`/`<script>` 内容）
- Modify: `src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerSyntaxHighlighter.kt`（叠加 CSS/JS 层）
- Test: `src/test/kotlin/com/freemarkerplus/lexer/FreemarkerLexerTest.kt`（加 style/script 用例）

**Interfaces:**
- Consumes: `FreemarkerLexer`、`FreemarkerTokenTypes`、`FreemarkerSyntaxHighlighter`
- Produces: `FreemarkerTokenTypes.STYLE_DATA`、`FreemarkerTokenTypes.SCRIPT_DATA`；高亮器叠加 CSS/JS 层

- [ ] **Step 1: 加 token 类型**

在 `FreemarkerTokenTypes` object 内、`BAD_CHARACTER` 之后追加：

```kotlin
    @JvmField val STYLE_DATA = FreemarkerTokenType("STYLE_DATA")
    @JvmField val SCRIPT_DATA = FreemarkerTokenType("SCRIPT_DATA")
```

- [ ] **Step 2: 加 lexer 测试用例（先失败）**

在 `FreemarkerLexerTest` 内追加：

```kotlin
    fun testStyleBlockContentIsStyleData() {
        val text = "<style>.a { color: red; }</style>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,   // <style>
                FreemarkerTokenTypes.STYLE_DATA,          // .a { color: red; }
                FreemarkerTokenTypes.TEMPLATE_DATA    // </style>
            ),
            types(text)
        )
    }

    fun testScriptBlockContentIsScriptData() {
        val text = "<script>var x = 1;</script>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,   // <script>
                FreemarkerTokenTypes.SCRIPT_DATA,         // var x = 1;
                FreemarkerTokenTypes.TEMPLATE_DATA    // </script>
            ),
            types(text)
        )
    }
```

- [ ] **Step 3: 运行测试确认失败**

```bash
./gradlew test --tests "com.freemarkerplus.lexer.FreemarkerLexerTest"
```

预期：新增 2 个测试失败（当前 lexer 把 `<style>...` 整体输出为单个 `TEMPLATE_DATA`）。

- [ ] **Step 4: 扩展 `FreemarkerLexer`**

在 `Mode` 枚举加两个值，并新增/修改以下方法。

（1）替换 `Mode` 枚举：

```kotlin
    private enum class Mode { DATA, INTERPOLATION, TAG, STYLE, SCRIPT }
```

（2）替换 `advance()` 的 `when` 分支：

```kotlin
        when (mode) {
            Mode.DATA -> advanceData()
            Mode.INTERPOLATION -> advanceInterpolation()
            Mode.TAG -> advanceTag()
            Mode.STYLE -> advanceEmbeddedContent("style", FreemarkerTokenTypes.STYLE_DATA)
            Mode.SCRIPT -> advanceEmbeddedContent("script", FreemarkerTokenTypes.SCRIPT_DATA)
        }
```

（3）替换 `advanceData()`，在注释分支之前插入 style/script 检测：

```kotlin
    private fun advanceData() {
        when {
            startsWithIgnoreCase(pos, "<style") -> lexEmbeddedTagStart(Mode.STYLE)
            startsWithIgnoreCase(pos, "<script") -> lexEmbeddedTagStart(Mode.SCRIPT)
            startsWith(pos, "<#--") -> {
                tokenType = FreemarkerTokenTypes.COMMENT
                val end = indexOf(pos, "-->")
                pos = if (end >= 0) end + 3 else endOffset
                tokenEnd = pos
            }
            startsWith(pos, "</#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 3
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<@") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "\${") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.INTERPOLATION
            }
            else -> lexPlainData()
        }
    }
```

（4）新增以下方法（放在 `lexPlainData()` 之后）：

```kotlin
    private fun lexEmbeddedTagStart(contentMode: Mode) {
        val gt = indexOf(pos, ">")
        if (gt >= 0) {
            val selfClosing = gt > pos && buffer[gt - 1] == '/'
            pos = gt + 1
            tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
            tokenEnd = pos
            mode = if (selfClosing) Mode.DATA else contentMode
        } else {
            pos = endOffset
            tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
            tokenEnd = pos
        }
    }

    private fun advanceEmbeddedContent(tagName: String, dataType: IElementType) {
        val close = indexOfIgnoreCase(pos, "</$tagName")
        if (close >= 0) {
            tokenType = dataType
            tokenEnd = close
            pos = close
        } else {
            tokenType = dataType
            tokenEnd = endOffset
            pos = endOffset
        }
        mode = Mode.DATA
    }

    private fun startsWithIgnoreCase(offset: Int, text: String): Boolean {
        if (offset + text.length > endOffset) return false
        for (i in text.indices) {
            if (buffer[offset + i].lowercaseChar() != text[i].lowercaseChar()) return false
        }
        return true
    }

    private fun indexOfIgnoreCase(from: Int, text: String): Int {
        var i = from
        while (i <= endOffset - text.length) {
            if (startsWithIgnoreCase(i, text)) return i
            i++
        }
        return -1
    }
```

> `lowercaseChar()` 是 Kotlin 标准库 `Char` 扩展，用于大小写不敏感匹配 `<STYLE>` 等。

- [ ] **Step 5: 扩展高亮器叠加 CSS/JS 层**

替换 `FreemarkerSyntaxHighlighter.kt` 中 `getHighlightingLexer()` 与 `getTokenHighlights()`，并加 CSS/JS 高亮器引用。

（1）在 `htmlHighlighter` 之后新增：

```kotlin
    private val cssHighlighter: SyntaxHighlighter? by lazy {
        SyntaxHighlighterFactory.getSyntaxHighlighter(CSSLanguage.INSTANCE, project, virtualFile)
    }
    private val jsHighlighter: SyntaxHighlighter? by lazy {
        SyntaxHighlighterFactory.getSyntaxHighlighter(JavaScriptLanguage.INSTANCE, project, virtualFile)
    }
```

（2）在 import 区追加：

```kotlin
import com.freemarkerplus.lexer.FreemarkerTokenTypes
import com.intellij.lang.css.CSSLanguage
import com.intellij.lang.javascript.JavaScriptLanguage
```

（3）替换 `getHighlightingLexer()`：

```kotlin
    override fun getHighlightingLexer(): Lexer {
        val layered = LayeredLexer(FreemarkerLexer())
        htmlHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.TEMPLATE_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        cssHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.STYLE_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        jsHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.SCRIPT_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        return layered
    }
```

（4）替换 `getTokenHighlights()`：

```kotlin
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        FreemarkerColors.keyFor(tokenType)?.let { return pack(it) }
        htmlHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        cssHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        jsHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        return TextAttributesKey.EMPTY_ARRAY
    }
```

- [ ] **Step 6: 运行测试**

```bash
./gradlew test
```

预期：`BUILD SUCCESSFUL`，全部测试通过。

- [ ] **Step 7: 手动验证 CSS/JS 高亮**

更新 `sample.ftl`，加入 style/script 块，重新 `./gradlew runIde` 打开，确认 `<style>` 内 CSS 属性/选择器、`<script>` 内 JS 关键字/字符串获得平台配色。

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat: add embedded CSS/JS highlighting for style/script blocks"
```

---

## Task 5: 配色设置页

**目标：** 提供 `Settings → Editor → Color Scheme → Freemarker` 设置页，含分组、默认色与预览文本。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerColorSettingsPage.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`（加 `colorSettingsPage` 扩展点）

**Interfaces:**
- Consumes: `FreemarkerColors.*`、`FreemarkerSyntaxHighlighter`
- Produces: `FreemarkerColorSettingsPage`（`ColorSettingsPage` 实现）

- [ ] **Step 1: 写设置页**

`src/main/kotlin/com/freemarkerplus/highlighting/FreemarkerColorSettingsPage.kt`：

```kotlin
package com.freemarkerplus.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class FreemarkerColorSettingsPage : ColorSettingsPage {

    private val descriptors = arrayOf(
        AttributesDescriptor("Comment", FreemarkerColors.COMMENT),
        AttributesDescriptor("String", FreemarkerColors.STRING),
        AttributesDescriptor("Keyword", FreemarkerColors.KEYWORD),
        AttributesDescriptor("Directive name", FreemarkerColors.DIRECTIVE_NAME),
        AttributesDescriptor("Interpolation delimiters", FreemarkerColors.INTERPOLATION),
        AttributesDescriptor("Number", FreemarkerColors.NUMBER),
        AttributesDescriptor("Operator", FreemarkerColors.OPERATOR),
        AttributesDescriptor("Bad character", FreemarkerColors.BAD_CHARACTER)
    )

    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter = FreemarkerSyntaxHighlighter(null, null)

    override fun getDemoText(): String = DEMO_TEXT

    override fun getDisplayName(): String = "Freemarker"

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = descriptors

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    companion object {
        private val DEMO_TEXT = """
            |<#-- This is a comment -->
            |<#if user.age gt 18>
            |  <div class="card">
            |    <h1>Welcome, ${'$'}{user.name}!</h1>
            |    <style>
            |      .card { color: green; }
            |    </style>
            |    <script>
            |      var greeting = "hello";
            |    </script>
            |  </div>
            |</#if>
        """.trimMargin()
    }
}
```

- [ ] **Step 2: 注册扩展点（修改 `plugin.xml`）**

在 `</extensions>` 之前追加：

```xml
        <colorSettingsPage implementation="com.freemarkerplus.highlighting.FreemarkerColorSettingsPage"/>
```

- [ ] **Step 3: 验证设置页**

```bash
./gradlew runIde
```

在沙箱 IDE 打开 `Settings → Editor → Color Scheme → Freemarker`，预期：
- 出现 8 个属性分组（Comment / String / Keyword / …）
- 预览区显示 DEMO_TEXT 且注释灰、字符串绿、指令名着色

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: add Freemarker color settings page"
```

---

## Task 6: 集成验证、README 与发布产物

**目标：** 用丰富的样例文件做端到端验证，补齐 README，产出可分发的插件 zip。

**Files:**
- Create: `README.md`
- Create: `examples/demo.ftl`（覆盖全部高亮场景的样例）

**Interfaces:**
- Consumes: 全部已实现组件

- [ ] **Step 1: 写样例文件**

`examples/demo.ftl`：

```html
<#-- FreeMarker Plus demo -->
<#assign title = "Hello ${user.name}">

<!DOCTYPE html>
<html>
<head>
  <title>${title}</title>
  <style>
    .card { border: 1px solid #ccc; color: green; }
    /* a css comment */
  </style>
  <script>
    var count = 42; // a js comment
    function greet(name) { return "Hi " + name; }
  </script>
</head>
<body>
  <#if user.age gt 18>
    <div class="card" id="main">Welcome, ${user.name}!</div>
  <#else>
    <div class="card">Sorry, too young.</div>
  </#if>
  <#list items as item>
    <p>${item}</p>
  </#list>
</body>
</html>
```

- [ ] **Step 2: 端到端手动验证**

```bash
./gradlew runIde
```

在沙箱 IDE 打开 `examples/demo.ftl`，逐项核对：
1. `<#-- -->` 与 HTML/CSS/JS 注释 → 灰
2. `${...}`、`class="..."`、CSS `"..."`、JS `"..."` 字符串 → 绿
3. `<div>`/`<html>`/`<style>`/`<script>` 等标签 → 黄（平台 HTML 配色）
4. `<#if>`/`<#list>`/`<#assign>` 指令名、`<@>` 宏名 → 指令色
5. `style` 内 CSS、`script` 内 JS 关键字/数字/字符串 → 平台 CSS/JS 配色

- [ ] **Step 3: 写 README**

`README.md`：

```markdown
# Freemarker Plus

IntelliJ IDEA 插件：为 FreeMarker 模板（`.ftl`）提供语法高亮，支持 Freemarker、HTML、CSS、JavaScript。

## 功能（Phase 1）

- Freemarker 语法高亮：插值 `${...}`、指令 `<#...>`、宏 `<@...>`、注释 `<#-- -->`、字符串/数字/关键字/操作符
- HTML 高亮（含标签、属性值字符串、注释）
- `<style>` / `<script>` 内 CSS / JS 高亮
- 主题感知配色，可在 `Settings → Editor → Color Scheme → Freemarker` 自定义

## 构建

要求 JDK 21。使用 Gradle 包装器：

    ./gradlew buildPlugin   # 打包，产物在 build/distributions/
    ./gradlew runIde        # 启动沙箱 IDE 调试
```

- [ ] **Step 4: 打包并验证产物**

```bash
./gradlew clean buildPlugin
```

预期：`BUILD SUCCESSFUL`，产出 `build/distributions/freemarker-plus-0.1.0.zip`。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "docs: add README and demo sample"
```

---

## Self-Review 记录（写计划时已完成）

1. **Spec 覆盖**：spec §5.3 token 粒度 → Task 2；§6 高亮器 → Task 3；§7 配色页 → Task 5；§8 plugin.xml → Task 3/5；§9 工具链 → Task 1；§11 测试 → Task 2/3/4；§4 架构 → 全 Task。CSS/JS（spec §2.1「识别 HTML/CSS/JS」）→ Task 4。
2. **占位符扫描**：无 TBD/TODO；所有代码步骤含实际代码。
3. **类型一致性**：`FreemarkerTokenTypes.*`、`FreemarkerColors.*`、`FreemarkerLexer`、`FreemarkerSyntaxHighlighter`、`FreemarkerLanguage.INSTANCE` 命名全程一致；`Mode` 枚举扩展在 Task 4 明确给出替换代码。

## 已知 Phase 1 边界（如实说明）

- 未闭合的 `${` / `<#...>` 不会标红（`BAD_CHARACTER` 仅用于标签内非法字符）；完整的未闭合/语义错误检测依赖 Phase 2 的 PSI 解析器。
- CSS/JS 高亮通过 lexer 识别 `<style>`/`<script>` 文本边界实现，不依赖 PSI 注入；遇到属性值含 `>` 的极端标签（如 `<style data-x=">">`）边界判断可能不准，属可接受的 Phase 1 简化。
