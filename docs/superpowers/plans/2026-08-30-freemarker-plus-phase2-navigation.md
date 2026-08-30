# Freemarker Plus — Phase 2 代码导航 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `.ftl` 文件构建 PSI 树并实现代码导航：跳转到声明、查找引用、结构视图、折叠、面包屑、重命名。

**Architecture:** GrammarKit 生成解析器 + 扁平 FTL PSI：`.ftl` 整体由 FTL 解析器解析，HTML/CSS/JS 数据区为不透明 `TEMPLATE_DATA` 叶节点（不叠加 HTML PSI）。FTL 构造（`<#..>`/`<@..>`/`${..}`）解析为类型化 FTL PSI。引用通过 `PsiReferenceContributor` 注册，声明/引用由 PSI + 文件级索引驱动。

**Tech Stack:** Kotlin、Gradle Kotlin DSL、IntelliJ Platform Gradle Plugin 2.16.0、GrammarKit（`org.jetbrains.intellij.platform.grammarkit`）、JUnit4 + IntelliJ 测试框架。

**Spec:** `docs/superpowers/specs/2026-08-30-freemarker-plus-phase2-navigation-design.md`

## Global Constraints

- 平台版本：`2026.2`（build `262`）；`sinceBuild = 262`，`untilBuild = 262.*`
- 语言 ID：`FTL`；文件类型名：`FreeMarker Template`；扩展名：`ftl;ftlh;ftlx`（沿用 Phase 1）
- 源码语言：Kotlin；构建脚本：Kotlin DSL；基础包名：`com.freemarkerplus`
- PSI 类名统一 `Ftl` 前缀（对齐官方插件与 FreeMarker 命名习惯）
- 工具链：JDK 21（toolchain）、Gradle 9.7.1、IntelliJ Platform Gradle Plugin `2.16.0`、Kotlin `2.3.0`
- 保留 Phase 1 的 `lang.syntaxHighlighterFactory` 与 `colorSettingsPage`，不删除、不改行为
- 每个 Task 结束都必须 `git commit`；提交信息用 `feat:` / `test:` / `chore:` 前缀
- 本阶段**不写**：代码补全、语义检查（unresolved 红色波浪线）、格式化、Java 数据模型解析

---

## File Structure

```
freemarker-plus/
├── build.gradle.kts                                    # 增加 grammarKit 插件 + 生成任务
├── src/main/kotlin/com/freemarkerplus/
│   ├── lang/FreemarkerLanguage.kt                      # 已有，不改（FTL + TemplateLanguage）
│   ├── lang/FreemarkerFileType.kt                      # 已有，不改
│   ├── lexer/FreemarkerLexer.kt                        # Phase1 高亮 lexer，保留不动
│   ├── psi/
│   │   ├── Freemarker.bnf                              # GrammarKit 语法（源）
│   │   ├── FtlElementType.kt                           # IElementType 基类
│   │   ├── FtlTokenType.kt                             # token 类型基类
│   │   ├── FtlParserDefinition.kt                      # ParserDefinition
│   │   ├── FtlFile.kt                                  # PSI 文件根
│   │   ├── FtlFileViewProviderFactory.kt               # 单根 FileViewProvider（TemplateLanguage 必需，非 HTML 叠加）
│   │   └── FtlStringLiteral.kt                         # 字符串字面量（语法生成）
│   ├── reference/
│   │   ├── FtlReferenceContributor.kt                  # 引用注册
│   │   ├── FtlFileReference.kt                         # include/import 文件引用
│   │   ├── FtlMacroReference.kt                        # <@m> 宏/函数引用
│   │   ├── FtlVariableReference.kt                     # ${v} 变量引用
│   │   ├── FtlNamespaceReference.kt                    # ns.m 命名空间引用
│   │   ├── FtlStringManipulator.kt                     # 字符串引用文本/替换
│   │   └── FtlFileIndex.kt                             # 文件级宏/变量声明索引
│   └── navigation/
│       ├── FtlFindUsagesProvider.kt                    # 查找引用描述
│       ├── FtlFindUsagesHandlerFactory.kt              # 查找引用处理器
│       ├── FtlDefinitionSearcher.kt                    # 跨文件定义搜索
│       ├── FtlMethodUsageSearcher.kt                   # 跨文件方法引用搜索
│       ├── FtlStructureViewBuilderProvider.kt          # 结构视图工厂
│       ├── FtlStructureViewModel.kt                    # 结构视图模型
│       ├── FtlFoldingBuilder.kt                        # 折叠
│       ├── FtlBreadcrumbsInfoProvider.kt               # 面包屑
│       └── FtlRenameProcessor.kt                       # 重命名
├── src/main/resources/META-INF/plugin.xml              # 新增导航扩展点
└── src/test/kotlin/com/freemarkerplus/
    ├── psi/FtlParserTest.kt                            # PSI 结构测试
    ├── reference/FtlReferenceTest.kt                   # 引用解析测试
    ├── navigation/FtlStructureViewTest.kt              # 结构视图/折叠测试
    └── navigation/FtlFindUsagesTest.kt                 # 查找引用/重命名测试
```

> 说明：GrammarKit 生成 Java 产物 `FtlParser.java`、`_FtlLexer.java`、`FtlElementTypes.java`（含 token 常量，无独立 `FtlTokenTypes`）及 PSI 接口/实现，落入 `src/main/gen/`；`Freemarker.bnf`、`_FtlLexer.flex`、`FtlElementType.kt`、`FtlTokenType.kt`、`FtlParserDefinition`、`FtlFile` 及手写补充类进 `src/`。

---

## Task 1: GrammarKit 工具链 + BNF 语法 + 生成解析器

**目标：** 接入 GrammarKit 插件，写好 `Freemarker.bnf`，`./gradlew generateFtlParser`（或等价任务）成功生成 `FtlParser`/`FtlLexer`，且 `./gradlew compileKotlin` 通过（尚无行为，仅编译通过）。

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/main/kotlin/com/freemarkerplus/psi/Freemarker.bnf`
- Create: `src/main/kotlin/com/freemarkerplus/psi/FtlElementType.kt`
- Create: `src/main/kotlin/com/freemarkerplus/psi/FtlTokenType.kt`

**Interfaces:**
- Produces:
  - `FtlElementType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE)`
  - `FtlTokenType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE)`
  - 生成的 `com.freemarkerplus.psi.FtlParser`、`com.freemarkerplus.psi._FtlLexer`、`com.freemarkerplus.psi.FtlElementTypes`（含 token 常量）

- [ ] **Step 1: 确定 grammarKit 插件版本**

IPGP 2.x 官方语法套件已合并为 `org.jetbrains.intellij.platform.grammarkit`。在 `build.gradle.kts` 的 `plugins {}` 加入，版本与 IPGP 对齐为 `2.16.0`。若该版本不存在，按 [Gradle Grammar-Kit Plugin 文档](https://plugins.jetbrains.com/docs/intellij/tools-gradle-grammar-kit-plugin.html) 取最新兼容版本；极不兼容时回退到独立的 `org.jetbrains.grammarkit` `2022.3.2.2` 并把生成产物提交进源码树。

- [ ] **Step 2: 改 `build.gradle.kts`**

```kotlin
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.grammarkit.GenerateParserTask

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
        bundledPlugin("com.intellij.properties")
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
    buildSearchableOptions { enabled = false }
}
```

> 若 GrammarKit 插件版本号不是 `2.16.0`，以 Step 1 确认的版本替换，其余不动。`bundledPlugin("com.intellij.properties")` 供后续 `.properties` 相关能力使用（与官方插件依赖一致），本 Task 暂可省略——若 `runIde` 校验报缺失依赖再补。

- [ ] **Step 3: 写 `FtlElementType.kt`**

```kotlin
package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

open class FtlElementType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FtlElementType.$debugName"
}
```

- [ ] **Step 4: 写 `FtlTokenType.kt`**

```kotlin
package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

open class FtlTokenType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FtlTokenType.$debugName"
}
```

- [ ] **Step 5: 写 `Freemarker.bnf`**

```bnf
{
  generate=[psi="yes" tokens="yes"]
  parserClass="com.freemarkerplus.psi.FtlParser"
  parserUtilClass="com.freemarkerplus.psi.FtlParserUtil"
  psiClassPrefix="Ftl"
  psiImplClassSuffix="Impl"
  psiPackage="com.freemarkerplus.psi"
  psiImplPackage="com.freemarkerplus.psi.impl"
  elementTypeHolderClass="com.freemarkerplus.psi.FtlElementTypes"
  elementTypeClass="com.freemarkerplus.psi.FtlElementType"
  tokenTypeClass="com.freemarkerplus.psi.FtlTokenType"
  tokens=[
    INCLUDE="include" IMPORT="import" ASSIGN="assign" LOCAL="local" GLOBAL="global"
    MACRO="macro" FUNCTION="function" LIST="list" IF="if" ELSEIF="elseif" ELSE="else"
    SWITCH="switch" CASE="case" DEFAULT="default" BREAK="break" AS="as"
    OPEN_TAG="<#" CLOSE_TAG="</#" OPEN_MACRO="<@" CLOSE_MACRO="</@"
    OPEN_INTERPOLATION="${" OPEN_LEGACY="#{" CLOSE_BRACE="}"
    TAG_END=">" COMMENT_START="<#--" COMMENT_END="-->"
    DOT="." COMMA="," ASSIGN_OP="=" LPAREN="(" RPAREN=")"
    IDENTIFIER="regexp:[a-zA-Z_][a-zA-Z0-9_]*"
    STRING_LITERAL="regexp:('([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\")"
    NUMBER="regexp:[0-9]+(\.[0-9]+)?"
    TEMPLATE_DATA="regexp:([^<]|<(?![#@/]))+"
  ]
}

ftlFile          ::= outer_element*
outer_element    ::= COMMENT | include_directive | import_directive | assign_directive
                   | macro_directive | function_directive | list_directive
                   | macro_call | interpolation | generic_directive | TEMPLATE_DATA

COMMENT          ::= COMMENT_START COMMENT_END

// <#include "a.ftl">
include_directive ::= OPEN_TAG INCLUDE string_literal TAG_END
// <#import "a.ftl" as ns>
import_directive  ::= OPEN_TAG IMPORT string_literal AS identifier TAG_END
// <#assign name = expr> / <#local ...> / <#global ...>
assign_directive  ::= OPEN_TAG (ASSIGN | LOCAL | GLOBAL) identifier ASSIGN_OP expression TAG_END
// <#macro name ...>
macro_directive   ::= OPEN_TAG MACRO identifier attribute* TAG_END
// <#function name ...>
function_directive::= OPEN_TAG FUNCTION identifier attribute* TAG_END
// <#list expr as name>
list_directive    ::= OPEN_TAG LIST expression AS identifier TAG_END

// <@name .../> 与 </@name>
macro_call        ::= OPEN_MACRO identifier attribute* TAG_END
                   | CLOSE_MACRO identifier TAG_END

// 其余指令（if/elseif/else/switch/case/default/break/自定义指令）走通用规则
generic_directive ::= OPEN_TAG directive_name attribute* TAG_END
                    | CLOSE_TAG directive_name TAG_END
directive_name    ::= IF | ELSEIF | ELSE | SWITCH | CASE | DEFAULT | BREAK | IDENTIFIER

interpolation     ::= OPEN_INTERPOLATION expression CLOSE_BRACE
                    | OPEN_LEGACY expression CLOSE_BRACE

expression        ::= primary (DOT primary)*
primary           ::= identifier
                    | string_literal
                    | NUMBER
                    | LPAREN expression RPAREN
attribute         ::= identifier ASSIGN_OP expression
                    | expression

identifier        ::= IDENTIFIER
string_literal    ::= STRING_LITERAL
```

- [ ] **Step 6: 运行生成任务**

```bash
./gradlew generateFtlParser
```

预期：在 `src/main/gen/` 生成 `FtlParser.java`、`_FtlLexer.java`、`FtlElementTypes.java`（含 token 常量）等，无 BNF 报错。`TEMPLATE_DATA` 已作为正则 token 声明（数据区叶节点）；若 BNF 报错，按错误信息修正正则或 token 顺序。

- [ ] **Step 7: 编译**

```bash
./gradlew compileKotlin
```

预期：`BUILD SUCCESSFUL`（生成的 parser 与手写类型基类可编译）。

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "chore: add GrammarKit toolchain and initial FTL grammar"
```

---

## Task 2: FtlParserDefinition + FtlFile + 注册 PSI 入口

**目标：** 让 `.ftl` 文件真正产生 PSI 树——注册 `ParserDefinition`，定义 `FtlFile`，`plugin.xml` 挂上 `lang.parserDefinition`。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/psi/FtlParserDefinition.kt`
- Create: `src/main/kotlin/com/freemarkerplus/psi/FtlFile.kt`
- Create: `src/main/kotlin/com/freemarkerplus/psi/FtlFileViewProviderFactory.kt`（单根 FileViewProvider——`TemplateLanguage` 语言无 `lang.fileViewProviderFactory` 时 `createFileFromText` 返回 null）
- Modify: `src/main/grammar/_FtlLexer.flex`（改为状态机 lexer：YYINITIAL/TAG/INTERPOLATION/COMMENT；否则 TEMPLATE_DATA 贪婪吞掉指令/插值内容）
- Modify: `build.gradle.kts`（仅加 `testBundledPlugin("intellij.libraries.misc.plugin")`，2026.2 EAP 测试模块解析 bug 的 workaround，见 IJPL-248701）
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/psi/FtlParserTest.kt`

**Interfaces:**
- Consumes: `FtlParser`、`FtlLexer`、`FtlElementTypes`（Task 1）、`FreemarkerLanguage.INSTANCE`、`FreemarkerFileType.INSTANCE`（Phase 1）
- Produces:
  - `FtlParserDefinition : ParserDefinition`
  - `FtlFile : PsiFileBase`（`getFileType()` 返回 `FreemarkerFileType.INSTANCE`）

- [ ] **Step 1: 写 `FtlParserDefinition.kt`**

```kotlin
package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class FtlParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = FlexAdapter(_FtlLexer())

    override fun createParser(project: Project): PsiParser = FtlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet =
        TokenSet.create(FtlElementTypes.COMMENT_START, FtlElementTypes.COMMENT_END)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(FtlElementTypes.STRING)

    override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement =
        FtlElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = FtlFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
        ParserDefinition.SpaceRequirements.MAY

    companion object {
        val FILE = IFileElementType(FreemarkerLanguage.INSTANCE)
    }
}
```

> `FtlElementTypes.Factory` 由 GrammarKit 生成（`psiClassPrefix="Ftl"` + `elementTypeHolderClass`）。若生成的工厂类名不同，按生成产物调整 `createElement` 实现。

- [ ] **Step 2: 写 `FtlFile.kt`**

```kotlin
package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerFileType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class FtlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FreemarkerLanguage.INSTANCE) {
    override fun getFileType(): FileType = FreemarkerFileType.INSTANCE
}
```

- [ ] **Step 3: 注册 `lang.parserDefinition`（改 `plugin.xml`）**

在 `<extensions defaultExtensionNs="com.intellij">` 内、`fileType` 之前插入：

```xml
<lang.parserDefinition language="FTL"
                       implementationClass="com.freemarkerplus.psi.FtlParserDefinition"/>
```

- [ ] **Step 4: 写 PSI 结构测试（先失败）**

`src/test/kotlin/com/freemarkerplus/psi/FtlParserTest.kt`：

```kotlin
package com.freemarkerplus.psi

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.freemarkerplus.lang.FreemarkerLanguage

class FtlParserTest : BasePlatformTestCase() {

    private fun parse(text: String): FtlFile {
        val factory = PsiFileFactory.getInstance(project)
        return factory.createFileFromText("test.ftl", FreemarkerLanguage.INSTANCE, text) as FtlFile
    }

    fun testAssignDirectiveParsed() {
        val file = parse("<#assign user = \"a\">")
        val assign = PsiTreeUtil.findChildOfType(file, FtlAssignDirective::class.java)
        assertNotNull(assign)
        assertEquals("user", assign?.identifier?.text)
    }

    fun testInterpolationParsed() {
        val file = parse("hello \${user.name}")
        val interp = PsiTreeUtil.findChildOfType(file, FtlInterpolation::class.java)
        assertNotNull(interp)
        assertTrue(interp!!.text.contains("user.name"))
    }
}
```

- [ ] **Step 5: 运行测试确认失败/通过并迭代**

```bash
./gradlew test --tests "com.freemarkerplus.psi.FtlParserTest"
```

预期：编译并运行通过（能生成 PSI 即可；精确结构断言在 Task 5/6 引用测试里细化）。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: register FTL parser definition and PSI file root"
```

---

## Task 3: 数据区叶节点 + 高亮兼容性验证

**目标：** 确认 `.ftl` 的 PSI 树中 FTL 构造与数据区（HTML）共存：数据区是 `TEMPLATE_DATA` 叶节点（Task 1 的 BNF 已定义），FTL 构造是类型化节点；同时确认 Phase 1 高亮不回归。

**Files:**
- Test: `src/test/kotlin/com/freemarkerplus/psi/FtlParserTest.kt`（加数据区用例）

**Interfaces:**
- Consumes: `FtlParserDefinition`、`FtlFile`（Task 2）、`FtlElementTypes.TEMPLATE_DATA`（Task 1 生成）
- Produces: 无新组件（验证任务）

- [ ] **Step 1: 加数据区/混排 PSI 测试（先失败）**

在 `FtlParserTest.kt` 追加：

```kotlin
fun testHtmlDataIsTemplateDataLeaf() {
    val file = parse("<div>hello</div>")
    val dataLeaves = PsiTreeUtil.collectElementsOfType(file, com.intellij.psi.PsiElement::class.java)
        .filter { it.node?.elementType == FtlElementTypes.TEMPLATE_DATA }
    assertTrue(dataLeaves.isNotEmpty())
}

fun testMixedContentParsed() {
    val file = parse("<div>\${user}</div><#if x>y</#if>")
    assertNotNull(PsiTreeUtil.findChildOfType(file, FtlInterpolation::class.java))
    assertNotNull(PsiTreeUtil.findChildOfType(file, FtlGenericDirective::class.java))
}
```

- [ ] **Step 2: 运行测试确认通过**

```bash
./gradlew test --tests "com.freemarkerplus.psi.FtlParserTest"
```

预期：通过。若 `TEMPLATE_DATA` 正则把 FTL 定界符吞掉（如 `<#if` 被判为数据），调整 BNF 的 `TEMPLATE_DATA` 正则，保证 `<#`/`<@`/`</#`/`</@`/`${`/`#{` 优先于数据匹配。

- [ ] **Step 3: 手动验证高亮不回归**

```bash
./gradlew runIde
```

打开 `examples/demo.ftl`（Phase 1 样例），确认 Freemarker/HTML/CSS/JS 高亮与 Phase 1 一致（高亮走独立的 Phase 1 lexer，与 PSI 无关）。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "test: verify template-data leaves coexist with FTL PSI, highlighting intact"
```

---

## Task 4: 文件引用（#include / #import → 文件）

**目标：** 在 `<#include "a.ftl">` / `<#import "a.ftl" as ns>` 的字符串上提供文件引用，`Ctrl+B` 跳转到目标 `.ftl` 文件。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlReferenceContributor.kt`
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlFileReference.kt`
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlStringManipulator.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/reference/FtlReferenceTest.kt`

**Interfaces:**
- Consumes: `FtlIncludeDirective`/`FtlImportDirective`/`FtlStringLiteral` PSI 元素（Task 2/3 生成）
- Produces:
  - `FtlReferenceContributor : PsiReferenceContributor`（`registerReferenceProviders`）
  - `FtlFileReference : PsiReferenceBase<PsiElement>`（`resolve()` 返回目标文件，`getVariants()` 返回候选文件）
  - `FtlStringManipulator : ElementManipulator<FtlStringLiteral>`（`getRangeInElement`/`handleContentChange`）

- [ ] **Step 1: 写 `FtlStringManipulator.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.util.IncorrectOperationException

class FtlStringManipulator : AbstractElementManipulator<FtlStringLiteral>() {
    override fun handleContentChange(element: FtlStringLiteral, range: TextRange, newContent: String): FtlStringLiteral? {
        val text = element.text
        val newText = text.substring(0, range.startOffset) + newContent + text.substring(range.endOffset)
        return element.replace(com.intellij.psi.util.PsiUtilCore.getElementFactory(element.project)
            .createExpressionFromText(newText, element)) as FtlStringLiteral
    }

    override fun getRangeInElement(element: FtlStringLiteral): TextRange {
        // 去掉首尾引号，使引用指向引号内的路径文本
        val text = element.text
        return if (text.length >= 2 && (text.first() == '"' || text.first() == '\'')) {
            TextRange(1, text.length - 1)
        } else {
            TextRange(0, text.length)
        }
    }
}
```

> `createExpressionFromText` 可能不是 `FtlFile` 的公开 API；若不可用，改用 `PsiFileFactory` 创建临时 `FtlFile` 再取首个 `FtlStringLiteral` 替换。实现时以平台 API 为准。

- [ ] **Step 2: 写 `FtlFileReference.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

class FtlFileReference(element: FtlStringLiteral, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement) {

    private val delegate: FileReferenceSet by lazy {
        FileReferenceSet(myElement.containingFile, myElement, rangeInElement.startOffset, this, true, true, null)
    }

    override fun resolve(): PsiElement? = delegate.allReferences.lastOrNull()?.resolve()

    override fun getVariants(): Array<Any> = delegate.allReferences.lastOrNull()?.variants ?: emptyArray()
}
```

> `FileReferenceSet` 是平台标准文件引用实现（官方插件 `getFileReferences` 即返回 `FileReference[]`）。构造参数以平台 SDK 为准。

- [ ] **Step 3: 写 `FtlReferenceContributor.kt`（先只挂文件引用）**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlImportDirective
import com.freemarkerplus.psi.FtlIncludeDirective
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class FtlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlStringLiteral::class.java),
            com.intellij.psi.PsiReferenceProvider { element, _, _ ->
                if (isFileReferenceHost(element)) arrayOf(FtlFileReference(element, rangeOf(element))) else emptyArray()
            }
        )
    }

    private fun isFileReferenceHost(element: FtlStringLiteral): Boolean {
        val parent = element.parent
        // parent 是 include/import 指令内的 string_literal
        return parent is FtlIncludeDirective || parent is FtlImportDirective
    }

    private fun rangeOf(element: FtlStringLiteral) = com.intellij.openapi.util.TextRange(1, element.textLength - 1)
}
```

> `FtlIncludeDirective`/`FtlImportDirective` 由 Task 1 的类型化语法生成（`include_directive`/`import_directive` 规则），其访问器为 `getStringLiteral(): FtlStringLiteral`。

- [ ] **Step 4: 注册扩展点（改 `plugin.xml`）**

```xml
<psi.referenceContributor language="FTL"
                          implementation="com.freemarkerplus.reference.FtlReferenceContributor"/>
<lang.elementManipulator forClass="com.freemarkerplus.psi.FtlStringLiteral"
                         implementationClass="com.freemarkerplus.reference.FtlStringManipulator"/>
```

- [ ] **Step 5: 写文件引用测试（先失败）**

`src/test/kotlin/com/freemarkerplus/reference/FtlReferenceTest.kt`：

```kotlin
package com.freemarkerplus.reference

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlReferenceTest : BasePlatformTestCase() {

    fun testIncludeResolvesToFile() {
        val inc = myFixture.addFileToProject("inc.ftl", "<#-- inc -->")
        myFixture.configureByText("main.ftl", "<#include \"inc.ftl\">")
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals(inc.virtualFile, ref?.resolve()?.containingFile?.virtualFile)
    }
}
```

- [ ] **Step 6: 运行测试确认失败→实现→通过**

```bash
./gradlew test --tests "com.freemarkerplus.reference.FtlReferenceTest"
```

预期：先失败（无引用/解析不到），修正实现后 `resolve()` 命中 `inc.ftl`。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat: add file references for include/import directives"
```

---

## Task 5: 宏/函数引用（<@m> → <#macro m>，同文件）

**目标：** `<@hello/>`、`<@hello a=1>` 与 `hello(...)` 调用能跳转到同文件的 `<#macro hello>` / `<#function hello>` 声明。

**Files:**
- Modify: `src/main/kotlin/com/freemarkerplus/reference/FtlReferenceContributor.kt`
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlMacroReference.kt`
- Test: `src/test/kotlin/com/freemarkerplus/reference/FtlReferenceTest.kt`（加用例）

**Interfaces:**
- Consumes: `FtlMacroDirective`/`FtlFunctionDirective` 声明元素、`FtlElementTypes`（Task 2/3）
- Produces:
  - `FtlMacroReference : PsiPolyVariantReferenceBase<PsiElement>`（`multiResolve` 返回声明）
  - 手写辅助：`FtlPsiUtil.findMacroDeclaration(file, name)` 收集同文件 `<#macro name>`/`<#function name>`

- [ ] **Step 1: 写声明收集辅助 `FtlPsiUtil.kt`**

```kotlin
package com.freemarkerplus.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object FtlPsiUtil {
    // 收集指定名字的宏/函数声明，返回名字标识符（跳转目标高亮名字）
    fun findMacroDeclarations(file: FtlFile, name: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        PsiTreeUtil.processElements(file) { el ->
            val ident = when (el) {
                is FtlMacroDirective -> el.identifier     // <#macro name>
                is FtlFunctionDirective -> el.identifier  // <#function name>
                else -> null
            }
            if (ident != null && ident.text == name) result.add(ident)
            true
        }
        return result
    }
}
```

> `FtlMacroDirective`/`FtlFunctionDirective` 由 Task 1 的类型化语法生成（`macro_directive`/`function_directive` 规则，GrammarKit 按规则名命名类），生成的访问器为 `getIdentifier(): FtlIdentifier`。

- [ ] **Step 2: 写 `FtlMacroReference.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class FtlMacroReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.text
        val file = element.containingFile as? FtlFile ?: return emptyArray()
        return FtlPsiUtil.findMacroDeclarations(file, name)
            .map { com.intellij.psi.PsiElementResolveResult(it) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
```

- [ ] **Step 3: 在 `FtlReferenceContributor` 注册宏引用**

在 `registerReferenceProviders` 中追加（替换 Step 3 的占位判断为类型化判断）：

```kotlin
registrar.registerReferenceProvider(
    PlatformPatterns.psiElement(FtlIdentifier::class.java)
        .withParent(FtlMacroCall::class.java),
    com.intellij.psi.PsiReferenceProvider { element, _, _ ->
        // element 是 <@name 里的 name；</@name 的闭合名也指向同一宏，允许导航
        if (element.parent.text.startsWith("<@")) {
            arrayOf(FtlMacroReference(element, TextRange(0, element.textLength)))
        } else emptyArray()
    }
)
```

> `FtlIdentifier`/`FtlMacroCall` 由 Task 1 语法生成（`identifier`/`macro_call` 规则）。

- [ ] **Step 4: 写宏引用测试**

在 `FtlReferenceTest.kt` 追加：

```kotlin
fun testMacroCallResolvesToDefinition() {
    myFixture.configureByText("main.ftl", "<#macro hello>hi</#macro>\n<@hello/>")
    val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    assertNotNull(ref)
    assertTrue(ref!!.element.text == "hello")
}
```

> 光标定位到 `<@hello/>` 的 `hello`：测试里用 `myFixture.configureByText` 后手动把 caret 移到 `hello` 处（`myFixture.editor.caretModel.moveToOffset(...)`）。

- [ ] **Step 5: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.reference.FtlReferenceTest"
git add -A
git commit -m "feat: add macro/function call references (same-file)"
```

---

## Task 6: 变量引用（${v} → 声明，同文件）

**目标：** `${user}`、`<#if user>` 中的 `user` 能跳转到同文件 `<#assign user=...>` / `<#local>` / `<#global>` / `<#list ... as user>` / `<#macro m(user)>` 的声明。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlVariableReference.kt`
- Modify: `src/main/kotlin/com/freemarkerplus/reference/FtlReferenceContributor.kt`
- Test: `src/test/kotlin/com/freemarkerplus/reference/FtlReferenceTest.kt`（加用例）

**Interfaces:**
- Consumes: `FtlAssignDirective`/`FtlListDirective`/`FtlMacroDirective` 参数（Task 2/3）、`FtlPsiUtil`（Task 5）
- Produces:
  - `FtlVariableReference : PsiPolyVariantReferenceBase<PsiElement>`（`multiResolve` 返回变量声明）
  - `FtlPsiUtil.findVariableDeclarations(file, name)` 收集变量声明

- [ ] **Step 1: 在 `FtlPsiUtil` 加变量声明收集**

```kotlin
fun findVariableDeclarations(file: FtlFile, name: String): List<PsiElement> {
    val result = mutableListOf<PsiElement>()
    PsiTreeUtil.processElements(file) { el ->
        val ident = when (el) {
            is FtlAssignDirective -> el.identifier  // assign/local/global 赋值名
            is FtlListDirective -> el.identifier    // list 的 as 循环变量
            else -> null
        }
        if (ident != null && ident.text == name) result.add(ident)
        true
    }
    return result
}
```

- [ ] **Step 2: 写 `FtlVariableReference.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class FtlVariableReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.text
        val file = element.containingFile as? FtlFile ?: return emptyArray()
        return FtlPsiUtil.findVariableDeclarations(file, name)
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
```

- [ ] **Step 3: 在 `FtlReferenceContributor` 注册变量引用**

追加（匹配 `INTERPOLATION` 或表达式内的首段 `IDENTIFIER`，排除宏调用名与字符串）：

```kotlin
registrar.registerReferenceProvider(
    PlatformPatterns.psiElement(FtlIdentifier::class.java)
        .withParent(FtlPrimary::class.java)
        .withAncestor(0, 5, FtlInterpolation::class.java),
    com.intellij.psi.PsiReferenceProvider { element, _, _ ->
        // 只对限定名根段挂引用（${user.name} → user）；属性段 name 属 Phase 3 Java 数据模型
        val expr = PsiTreeUtil.getParentOfType(element, FtlExpression::class.java) ?: return@PsiReferenceProvider emptyArray()
        val rootPrimary = expr.firstChild
        if (rootPrimary === element.parent) {
            arrayOf(FtlVariableReference(element, TextRange(0, element.textLength)))
        } else emptyArray()
    }
)
```

- [ ] **Step 4: 写变量引用测试**

```kotlin
fun testVariableResolvesToAssign() {
    myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n\${user}")
    // caret 移到 ${user} 的 user
    myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("user", myFixture.file.text.indexOf("\${")))
    val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    assertNotNull(ref)
    assertTrue(ref!!.resolve() != null)
}
```

- [ ] **Step 5: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.reference.FtlReferenceTest"
git add -A
git commit -m "feat: add variable references (same-file)"
```

---

## Task 7: 跨文件导航（FtlFileIndex + 命名空间引用）

**目标：** 通过文件级索引实现跨文件跳转：`<#import "lib.ftl" as lib>` 后 `lib.hello` 能跳转到 `lib.ftl` 里的 `<#macro hello>`；跨文件 `<@hello>` 也能命中其他文件定义的宏。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlFileIndex.kt`
- Create: `src/main/kotlin/com/freemarkerplus/reference/FtlNamespaceReference.kt`
- Modify: `src/main/kotlin/com/freemarkerplus/reference/FtlMacroReference.kt`（接索引）
- Modify: `src/main/kotlin/com/freemarkerplus/reference/FtlReferenceContributor.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/reference/FtlReferenceTest.kt`（加跨文件用例）

**Interfaces:**
- Consumes: `FtlPsiUtil`（Task 5/6）、`FtlImportDirective`（Task 2/3）
- Produces:
  - `FtlFileIndex : FileBasedIndexExtension<String, List<FtlIndexInfo>>`
  - `FtlIndexInfo(name: String, type: String, filePath: String)` 数据类
  - `FtlNamespaceReference : PsiPolyVariantReferenceBase<PsiElement>`

- [ ] **Step 1: 写 `FtlFileIndex.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroDirective
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

data class FtlIndexInfo(val name: String, val type: String, val filePath: String)

class FtlFileIndex : FileBasedIndexExtension<String, List<FtlIndexInfo>>() {

    override fun getName(): ID<String, List<FtlIndexInfo>> = NAME

    override fun getVersion(): Int = 1

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<List<FtlIndexInfo>> = object : DataExternalizer<List<FtlIndexInfo>> {
        override fun save(out: DataOutput, value: List<FtlIndexInfo>) {
            out.writeInt(value.size)
            value.forEach { out.writeUTF(it.name); out.writeUTF(it.type); out.writeUTF(it.filePath) }
        }
        override fun read(input: DataInput): List<FtlIndexInfo> {
            val n = input.readInt()
            return (0 until n).map { FtlIndexInfo(input.readUTF(), input.readUTF(), input.readUTF()) }
        }
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension == "ftl" || file.extension == "ftlh" || file.extension == "ftlx"
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, List<FtlIndexInfo>, FileContent> =
        DataIndexer { inputData ->
            val result = HashMap<String, MutableList<FtlIndexInfo>>()
            val psiFile = inputData.psiFile as? FtlFile ?: return@DataIndexer result
            // 收集宏/函数/变量声明（遍历 PSI 树，与 FtlPsiUtil 的收集逻辑一致）
            collectDeclarations(psiFile, inputData.file.path).forEach { info ->
                result.getOrPut(info.name) { mutableListOf() }.add(info)
            }
            result
        }

    private fun collectDeclarations(file: FtlFile, path: String): List<FtlIndexInfo> {
        val out = mutableListOf<FtlIndexInfo>()
        PsiTreeUtil.processElements(file) { el ->
            when (el) {
                is FtlMacroDirective -> out.add(FtlIndexInfo(el.identifier.text, "macro", path))
                is FtlFunctionDirective -> out.add(FtlIndexInfo(el.identifier.text, "function", path))
                is FtlAssignDirective -> out.add(FtlIndexInfo(el.identifier.text, "variable", path))
                is FtlListDirective -> out.add(FtlIndexInfo(el.identifier.text, "loop", path))
                else -> {}
            }
            true
        }
        return out
    }

    companion object {
        val NAME: ID<String, List<FtlIndexInfo>> = ID.create("com.freemarkerplus.ftlDeclarations")
    }
}
```

> 遍历 PSI 树收集声明（与 `FtlPsiUtil` 的收集逻辑一致）；后续若与 `FtlDefinitionSearcher` 的收集重复，可合并去重。

- [ ] **Step 2: 写 `FtlNamespaceReference.kt`**

```kotlin
package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.indexing.FileBasedIndex

class FtlNamespaceReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        // element.text 形如 "lib.hello"：前半是命名空间，后半是成员名
        val text = element.text
        val dot = text.indexOf('.')
        if (dot <= 0) return emptyArray()
        val namespace = text.substring(0, dot)
        val member = text.substring(dot + 1)

        val importedFile = findImportedFile(element.containingFile as? FtlFile, namespace) ?: return emptyArray()
        // 在导入文件里查 member 的宏/变量声明
        return FileBasedIndex.getInstance()
            .getValues(FtlFileIndex.NAME, member, element.project)
            .filter { it.filePath == importedFile.virtualFile.path }
            .map { PsiElementResolveResult(findByRange(importedFile, member)) }
            .toTypedArray()
    }

    private fun findImportedFile(file: FtlFile?, namespace: String): FtlFile? {
        if (file == null) return null
        val imports = PsiTreeUtil.getChildrenOfType(file, FtlImportDirective::class.java) ?: return null
        val import = imports.firstOrNull { it.identifier.text == namespace } ?: return null
        val path = import.stringLiteral.text.trim('\'', '"')
        val vf = file.virtualFile.parent?.findChild(path) ?: return null
        return com.intellij.psi.PsiManager.getInstance(file.project).findFile(vf) as? FtlFile
    }

    private fun findByRange(file: FtlFile, member: String): PsiElement? =
        com.intellij.psi.util.PsiTreeUtil.findElementOfClassAtOffset(file, file.text.indexOf(member), PsiElement::class.java, false)

    override fun getVariants(): Array<Any> = emptyArray()
}
```

- [ ] **Step 3: 在 `FtlReferenceContributor` 注册命名空间引用**

追加（匹配含 `.` 的限定名标识符）：

```kotlin
registrar.registerReferenceProvider(
    PlatformPatterns.psiElement().withText(PlatformPatterns.string().matches("[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*")),
    com.intellij.psi.PsiReferenceProvider { element, _, _ ->
        arrayOf(FtlNamespaceReference(element, TextRange(0, element.textLength)))
    }
)
```

- [ ] **Step 4: 注册 `fileBasedIndex`（改 `plugin.xml`）**

```xml
<fileBasedIndex implementation="com.freemarkerplus.reference.FtlFileIndex"/>
```

- [ ] **Step 5: 写跨文件测试**

```kotlin
fun testNamespaceResolvesAcrossFiles() {
    myFixture.addFileToProject("lib.ftl", "<#macro hello>hi</#macro>")
    myFixture.configureByText("main.ftl", "<#import \"lib.ftl\" as lib>\n\${lib.hello}")
    // caret 到 lib.hello
    myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("lib.hello") + 1)
    val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    assertNotNull(ref)
    assertTrue(ref!!.resolve() != null)
}
```

- [ ] **Step 6: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.reference.FtlReferenceTest"
git add -A
git commit -m "feat: add cross-file declaration index and namespace references"
```

---

## Task 8: 查找引用（Find Usages）

**目标：** `Alt+F7` 对宏/变量/命名空间返回所有使用处，`Ctrl+B`（找定义）在跨文件场景下走 `definitionsSearch`。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlFindUsagesProvider.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlFindUsagesHandlerFactory.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlDefinitionSearcher.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlMethodUsageSearcher.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/navigation/FtlFindUsagesTest.kt`

**Interfaces:**
- Consumes: `FtlFileIndex`（Task 7）、`FtlPsiUtil`
- Produces:
  - `FtlFindUsagesProvider : FindUsagesProvider`
  - `FtlFindUsagesHandlerFactory : FindUsagesHandlerFactory`
  - `FtlDefinitionSearcher : QueryExecutorBase<PsiElement, PsiElement>`
  - `FtlMethodUsageSearcher : QueryExecutorBase<PsiReference, PsiElement>`

- [ ] **Step 1: 写 `FtlFindUsagesProvider.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class FtlFindUsagesProvider : FindUsagesProvider {
    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element.language.id == "FTL"

    override fun getHelpId(element: PsiElement): String = "reference.dialogs.findUsages"

    override fun getType(element: PsiElement): String = when {
        element.text.startsWith("<#macro") -> "Macro"
        element.text.startsWith("<#function") -> "Function"
        else -> "Variable"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        element.text.lines().firstOrNull()?.take(60) ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
```

- [ ] **Step 2: 写 `FtlFindUsagesHandlerFactory.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlFile
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement

class FtlFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean = element.language.id == "FTL"

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler =
        object : FindUsagesHandler(element) {}
}
```

- [ ] **Step 3: 写 `FtlDefinitionSearcher.kt` 与 `FtlMethodUsageSearcher.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.freemarkerplus.reference.FtlFileIndex
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex

class FtlDefinitionSearcher : QueryExecutorBase<PsiElement, PsiElement>() {
    override fun processQuery(sourceElement: PsiElement, consumer: Processor<in PsiElement>) {
        // 对「引用」源，找跨文件定义：sourceElement.text 为名字
        val name = sourceElement.text
        FileBasedIndex.getInstance().getValues(FtlFileIndex.NAME, name, sourceElement.project)
            .forEach { /* 定位到声明 PSI 元素并 consumer.process(decl) */ }
    }
}
```

```kotlin
package com.freemarkerplus.navigation

import com.freemarkerplus.reference.FtlFileIndex
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex

class FtlMethodUsageSearcher : QueryExecutorBase<PsiReference, PsiElement>() {
    override fun processQuery(sourceElement: PsiElement, consumer: Processor<in PsiReference>) {
        // 对「声明」源，找所有引用（引用名 == 声明名）
        val name = sourceElement.text
        FileBasedIndex.getInstance().getValues(FtlFileIndex.NAME, name, sourceElement.project)
            .forEach { /* 定位引用 PsiReference 并 consumer.process(ref) */ }
    }
}
```

> 两个 searcher 中「定位到声明/引用 PSI 元素」的精确实现依赖 Task 5/6/7 的类型化元素；本 Task 先用索引键名 + 文本定位打通，后续任务收敛。

- [ ] **Step 4: 注册扩展点（改 `plugin.xml`）**

```xml
<lang.findUsagesProvider language="FTL"
                         implementationClass="com.freemarkerplus.navigation.FtlFindUsagesProvider"/>
<findUsagesHandlerFactory implementation="com.freemarkerplus.navigation.FtlFindUsagesHandlerFactory"/>
<definitionsSearch implementation="com.freemarkerplus.navigation.FtlDefinitionSearcher"/>
<methodReferencesSearch implementation="com.freemarkerplus.navigation.FtlMethodUsageSearcher"/>
```

- [ ] **Step 5: 写查找引用测试**

`src/test/kotlin/com/freemarkerplus/navigation/FtlFindUsagesTest.kt`：

```kotlin
package com.freemarkerplus.navigation

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlFindUsagesTest : BasePlatformTestCase() {

    fun testFindUsagesForMacro() {
        myFixture.configureByText("main.ftl", "<#macro m>a</#macro>\n<@m/>\n<@m/>")
        val usages = myFixture.testFindUsages("main.ftl")
        assertTrue(usages.isNotEmpty())
    }
}
```

- [ ] **Step 6: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.navigation.FtlFindUsagesTest"
git add -A
git commit -m "feat: add find usages provider, handler, and searchers"
```

---

## Task 9: 结构视图 + 折叠 + 面包屑

**目标：** `Alt+7` 显示指令/宏结构；块级指令可折叠；编辑器顶部面包屑显示上下文。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlStructureViewBuilderProvider.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlStructureViewModel.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlFoldingBuilder.kt`
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlBreadcrumbsInfoProvider.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/navigation/FtlStructureViewTest.kt`

**Interfaces:**
- Consumes: `FtlFile`、指令 PSI 元素（Task 2/3）
- Produces:
  - `FtlStructureViewBuilderProvider : PsiStructureViewFactory`
  - `FtlStructureViewModel : TextEditorBasedStructureViewModel`
  - `FtlFoldingBuilder : FoldingBuilder`
  - `FtlBreadcrumbsInfoProvider : BreadcrumbsProvider`

- [ ] **Step 1: 写 `FtlStructureViewModel.kt` 与 `FtlStructureViewBuilderProvider.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlFile
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

class FtlStructureViewBuilderProvider : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
        object : StructureViewBuilder {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                FtlStructureViewModel(psiFile, editor)
        }
}

class FtlStructureViewModel(psiFile: PsiFile, editor: Editor?) :
    TextEditorBasedStructureViewModel(editor, psiFile) {

    override fun getRoot(): StructureViewTreeElement = object : PsiTreeElementBase<PsiElement>(psiFile) {
        override fun getPresentableText(): String? = psiFile.name
        override fun getChildrenBase(): Collection<StructureViewTreeElement> = collectDirectives(psiFile)
    }

    private fun collectDirectives(scope: PsiElement): Collection<StructureViewTreeElement> {
        val result = mutableListOf<StructureViewTreeElement>()
        PsiTreeUtil.processElements(scope) { el ->
            if (isDirectiveElement(el)) {
                result.add(object : PsiTreeElementBase<PsiElement>(el) {
                    override fun getPresentableText(): String? = el.text.lines().firstOrNull()?.take(60)
                    override fun getChildrenBase(): Collection<StructureViewTreeElement> = collectDirectives(el)
                })
            }
            true
        }
        return result
    }

    private fun isDirectiveElement(el: PsiElement): Boolean {
        val t = el.text
        return t.startsWith("<#") || t.startsWith("<@")
    }
}
```

- [ ] **Step 2: 写 `FtlFoldingBuilder.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlFile
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil

class FtlFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val file = node.psi as? FtlFile ?: return emptyArray()
        val descriptors = mutableListOf<FoldingDescriptor>()
        // 对成对块级指令生成折叠区间（open <#x> ... close </#x>）
        val openTags = PsiTreeUtil.collectElementsOfType(file, com.intellij.psi.PsiElement::class.java)
        // 简化：扫描文本找 <#if>/<#list>/<#macro>/<#function>/<#switch> 与其 </#x> 配对
        val text = file.text
        for (name in listOf("if", "list", "macro", "function", "switch")) {
            val open = "<#$name"
            val close = "</#$name"
            var from = text.indexOf(open)
            while (from >= 0) {
                val to = text.indexOf(close, from + open.length)
                if (to > from) descriptors.add(FoldingDescriptor(node, TextRange(from, to + close.length + 1)))
                from = text.indexOf(open, to + close.length)
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "<#…>"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
```

- [ ] **Step 3: 写 `FtlBreadcrumbsInfoProvider.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import com.freemarkerplus.lang.FreemarkerLanguage

class FtlBreadcrumbsInfoProvider : BreadcrumbsProvider {
    override fun isShownByDefault(): Boolean = true

    override fun getLanguages(): Array<Language> = arrayOf(FreemarkerLanguage.INSTANCE)

    override fun acceptElement(element: PsiElement): Boolean =
        element.text.startsWith("<#") || element.text.startsWith("<@")

    override fun getParent(element: PsiElement): PsiElement? = element.parent

    override fun getElementInfo(element: PsiElement): String =
        element.text.lines().firstOrNull()?.take(80) ?: element.text

    override fun getElementTooltip(element: PsiElement): String = element.text
}
```

- [ ] **Step 4: 注册扩展点（改 `plugin.xml`）**

```xml
<lang.psiStructureViewFactory language="FTL"
                              implementationClass="com.freemarkerplus.navigation.FtlStructureViewBuilderProvider"/>
<lang.foldingBuilder language="FTL"
                     implementationClass="com.freemarkerplus.navigation.FtlFoldingBuilder"/>
<breadcrumbsInfoProvider implementation="com.freemarkerplus.navigation.FtlBreadcrumbsInfoProvider"/>
```

- [ ] **Step 5: 写结构视图/折叠测试**

`src/test/kotlin/com/freemarkerplus/navigation/FtlStructureViewTest.kt`：

```kotlin
package com.freemarkerplus.navigation

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlStructureViewTest : BasePlatformTestCase() {

    fun testFoldingRegionsForIf() {
        myFixture.configureByText("main.ftl", "<#if x>\nhello\n</#if>")
        val builder = FtlFoldingBuilder()
        val root = myFixture.file.node
        val regions = builder.buildFoldRegions(root, myFixture.editor.document)
        assertTrue(regions.isNotEmpty())
    }
}
```

- [ ] **Step 6: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.navigation.FtlStructureViewTest"
git add -A
git commit -m "feat: add structure view, folding, and breadcrumbs"
```

---

## Task 10: 重命名

**目标：** `Shift+F6` 重命名宏/变量/命名空间时，同步更新所有引用处。

**Files:**
- Create: `src/main/kotlin/com/freemarkerplus/navigation/FtlRenameProcessor.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/freemarkerplus/navigation/FtlFindUsagesTest.kt`（加 rename 用例）

**Interfaces:**
- Consumes: 查找引用（Task 8）、`FtlStringManipulator`（Task 4）
- Produces: `FtlRenameProcessor : RenamePsiElementProcessor`

- [ ] **Step 1: 写 `FtlRenameProcessor.kt`**

```kotlin
package com.freemarkerplus.navigation

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo

class FtlRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean =
        element.language.id == "FTL"

    override fun findReferences(element: PsiElement, searchScope: com.intellij.psi.search.SearchScope, searchInCommentsAndStrings: Boolean): Collection<PsiReference> =
        super.findReferences(element, searchScope, searchInCommentsAndStrings)

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        // 声明与引用通过 findReferences 统一收集，此处无需额外处理
    }
}
```

- [ ] **Step 2: 注册扩展点（改 `plugin.xml`）**

```xml
<renamePsiElementProcessor implementation="com.freemarkerplus.navigation.FtlRenameProcessor"/>
```

- [ ] **Step 3: 写重命名测试**

在 `FtlFindUsagesTest.kt` 追加：

```kotlin
fun testRenameMacro() {
    myFixture.configureByText("main.ftl", "<#macro m>a</#macro>\n<@m/>")
    myFixture.renameElementAtCaret("m2")
    assertTrue(myFixture.file.text.contains("m2"))
}
```

- [ ] **Step 4: 运行测试 → 通过 → 提交**

```bash
./gradlew test --tests "com.freemarkerplus.navigation.FtlFindUsagesTest"
git add -A
git commit -m "feat: add rename support for FTL declarations"
```

---

## Task 11: 集成验证 + README + 发布产物

**目标：** 用样例文件端到端验证所有导航功能，更新 README，产出可分发 zip。

**Files:**
- Create: `examples/navigation.ftl`（覆盖 include/import/macro/function/assign/list/命名空间的样例）
- Modify: `README.md`

**Interfaces:**
- Consumes: 全部已实现组件

- [ ] **Step 1: 写 `examples/navigation.ftl`**

```html
<#-- FreeMarker Plus navigation demo -->
<#import "lib.ftl" as lib>
<#assign title = "Hello ${lib.greet(user)}">

<#macro card heading>
  <div class="card"><h2>${heading}</h2></div>
</#macro>

<#function greet name>
  <#return "Hi, " + name>
</#function>

<#list items as item>
  <@card heading=item.name/>
</#list>
```

- [ ] **Step 2: 端到端手动验证**

```bash
./gradlew runIde
```

在沙箱 IDE 打开 `examples/navigation.ftl`，逐项核对：
1. `Ctrl+B` 于 `lib.greet` → 跳 `lib.ftl` 的 `<#function greet>`；于 `<@card>` → 跳 `<#macro card>`；于 `${title}` → 跳 `<#assign title>`
2. `Alt+F7` 于 `card` → 列出 `<#macro card>` 与 `<@card>`
3. `Alt+7` → 结构视图列出 `import`/`assign`/`macro`/`function`/`list`
4. `<#if>`/`<#list>`/`<#macro>` 可折叠
5. 编辑器顶部面包屑显示所在指令

- [ ] **Step 3: 更新 README（新增 Phase 2 功能段）**

在 README 的 Features 小节后追加：

```markdown
## Code Navigation (Phase 2)

- **Go to declaration** (Ctrl+B) — `#include`/`#import` file paths, `<@macro>` calls, `${variable}` references, `ns.member` namespaces.
- **Find usages** (Alt+F7) — macros, functions, variables.
- **Structure view** (Alt+7) — directives, macros, includes.
- **Code folding** — `<#if>/<#list>/<#macro>/<#function>/<#switch>` blocks.
- **Breadcrumbs** and **rename** (Shift+F6).
```

- [ ] **Step 4: 打包验证**

```bash
./gradlew clean buildPlugin
```

预期：`BUILD SUCCESSFUL`，产出 `build/distributions/freemarker-plus-0.1.0.zip`（或 bump 到 `0.2.0`，见 Step 5 决定）。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "docs: add navigation demo and Phase 2 README"
```

---

## Self-Review 记录（写计划时已完成）

1. **Spec 覆盖**：spec §4 技术路线 → Task 1/3；§6 语法 → Task 1/3；§7 引用 → Task 4/5/6/7；§8 结构/折叠/面包屑 → Task 9；§9 查找引用/重命名 → Task 8/10；§10 索引 → Task 7；§11 plugin.xml → 各 Task 内；§12 测试 → 各 Task 测试步骤；§5 组件 → 贯穿全 Task。
2. **依赖顺序**：Task 1(parser) → 2(注册+PSI) → 3(数据区验证) → 4/5/6(同文件引用) → 7(跨文件) → 8(查找引用) → 9/10(结构/重命名) → 11(验证)。后项 Consumes 前项 Produces，无循环。
3. **已知技术风险（如实标注，实现时优先验证）**：
   - GrammarKit 插件（`org.jetbrains.intellij.platform.grammarkit`）在 IPGP 2.16.0 / Gradle 9.7.1 下的确切版本号需 Task 1 Step 1 实测钉死。
   - `FileReferenceSet` 构造签名、GrammarKit 生成的 PSI 工厂类名/访问器名需以 2026.2 SDK 实测校正（各 Task 已标注）。
   - 引用 pattern 与「声明/引用定位」在 Task 4–8 用文本/正则过渡实现，需在引入类型化 PSI 后收敛为 `FtlMacroDirective`/`FtlFunctionDirective`/`FtlAssignDirective` 等元素判断——这是刻意的「先打通、后收敛」策略，避免一上来卡在完整表达式语法。

## 已知 Phase 2 边界（如实说明）

- 表达式仅解析到「限定名」粒度，不含完整运算符/类型系统；`foo.bar.baz` 中间段的属性级导航不完整。
- 未解析引用**不标红**（inspection 属 Phase 3）；跳转/查找引用/重命名对「已声明」的符号工作。
- Java 数据模型（`${user}` 中的 `user` 是 Java 对象）不解析，仅解析 FTL 内声明的变量。
- 跨文件解析依赖索引，首次打开大项目时需索引完成。
