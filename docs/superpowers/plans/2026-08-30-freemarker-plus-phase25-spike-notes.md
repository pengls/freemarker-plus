# Freemarker Plus — Phase 2.5 Spike 结论（HTML/JS 导航迁移可行性）

- 日期：2026-08-30
- 状态：**Go（有条件）** — 核心架构（S2）已验证可行；JS 三场景（S3/S4/S5）机制上成立，但单元测试环境无法加载 JavaScript 插件，需 runIde 人工验收兜底。

---

## 1. Spike 目标回顾

验证「把 .ftl 的 HTML/CSS/JS 数据区从透明 TEMPLATE_DATA 叶节点迁移为官方同款模板语言机制（真实 HTML/JS PSI）」在 2026.2 平台是否可行，并确认 `onclick="login()"` 能导航到 JS 的 `login` 方法。

## 2. 关键平台事实（反编译 2026.2 平台与官方插件得出）

### 2.1 官方插件（IU 262.8665.258 自带）的架构 = 双文件模板语言模型

- `FtlFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements ConfigurableTemplateLanguageFileViewProvider`
  - **base language 文件**：FTL 文件（`FtlFile`），数据区为 `TEMPLATE_TEXT` 叶节点；
  - **template data language 文件**：HTML/XML `PsiFile`，根内容元素 = `TEMPLATE_DATA` chameleon
    （`FtlFileViewProvider.getContentElementType` 返回它）。
- `FtlFileElementTypes`：
  ```java
  OUTER_ELEMENT_TYPE = new OuterLanguageElementType("FTL_FRAGMENT", FtlLanguage.INSTANCE);
  TEMPLATE_DATA = new TemplateDataElementType("FTL_TEMPLATE_DATA", FtlLanguage.INSTANCE,
                   FtlElementTypes.TEMPLATE_TEXT, OUTER_ELEMENT_TYPE) {
      protected boolean isInsertionToken(...) { return FtlElementTypes.EL_STARTS.contains(tokenType); }
  };
  ```
- `FtlLexer`（LookAheadLexer 包装）对数据区文本输出 `TEMPLATE_TEXT` token，对 FTL 构造输出标记 token。

### 2.2 新 `TemplateDataElementType`（2026.2）

- 包路径：`com.intellij.psi.templateLanguages`（旧 `psi.impl.source.template` 已移除）。
- 构造：`(debugName, Language templateLanguage, IElementType templateElementType, IElementType outerElementType)`
  —— 不再是旧版「传 lexer + baseLanguage」。
- `parseContents`：用 **template language 的 lexer**（`createBaseLexer` → FTL lexer）扫全文；
  `tokenType == templateElementType`（数据文本 token）保留，其余为 outer ranges（FTL 构造挖洞）；
  把保留文本交给 **template data language（HTML）解析器** 解析出 HTML PSI；再把 FTL 构造以
  `OuterLanguageElement` 插回。**要求所在文件的 view provider 是 `TemplateLanguageFileViewProvider`**。
- 官方插件只把 chameleon 用作 **HTML 文件的根内容元素**，**不**放在 FTL 语法里（FTL 树是
  `TEMPLATE_TEXT` 叶节点）——这是新 API 的设计意图（IFileElementType 不能直接当普通 token 用）。

### 2.3 JS 机制的两个门（都非 file-type 门控）

- **script 内容**：`html.scriptContentProvider` 按 **language** 注册；`language="HTML"` →
  `TemplateHtmlScriptContentProvider`（返回 `XmlElementType.HTML_EMBEDDED_CONTENT`，与真实 .html 一致）；
  JS 插件另注册 `language="JavaScript"` 等。数据文件语言是 HTML → 走同一结构。
- **事件属性注入**：`JSInheritedLanguagesConfigurableProvider.supportsJavaScriptInjections()` 默认
  **返回 true**（`com.intellij.lang.javascript.psi.JSInheritedLanguagesHelper`），不查 file type。
- **嵌入内容（embedment）**：`html.embeddedContentSupport`（JS 插件注册 `JSHtmlEmbeddedContentSupport`）
  是全局 EP，`BaseHtmlLexer` 对任意文件都会装配；`acceptEmbeddedContentProvider` 默认放行。

> 结论：JS 机制按 **language（HTML）+ 全局 EP** 生效，与宿主文件类型无关；我们的数据文件是
> `HtmlFileImpl`（language=HTML），结构与真实 .html 完全一致 → 平台 HTML/JS 导航会同等生效。

## 3. Spike 实测结果

### S2 — 数据区 → HTML PSI：**通过**（单元测试验证）

改动（原型代码，已在工作区）：
1. `_FtlLexer.flex` / `Freemarker.bnf`：数据区 token `TEMPLATE_DATA` → `TEMPLATE_TEXT`；
2. 新增 `FtlFileElementTypes`（`OUTER_ELEMENT_TYPE` + `TEMPLATE_DATA` chameleon）；
3. 新增 `FtlFileViewProvider`（双文件：base=FTL，template data=HTML/XML）；
4. `FtlFileViewProviderFactory` 改用 `FtlFileViewProvider`。

测试断言（`FtlSpikeHtmlTest.testDataFileExpandsToXmlTag`）：
```
viewProvider = FtlFileViewProvider
mainFile     = FtlFile (FTL)
file for HTML: com.intellij.psi.impl.source.html.HtmlFileImpl
  XmlTags=[div]   ← chameleon 展开出真实 HTML PSI ✓
```
- 属性也展开：`XmlAttribute(onclick)` 存在（S2 子项通过）。
- FTL 指令/插值（`FtlAssignDirective`/`FtlInterpolation`）与 HTML PSI 共存（通过）。

### S3/S4/S5 — JS PSI / onclick 解析 / 外部 .js：**机制成立，测试环境无法直接验证**

- 尝试 `testBundledPlugin("JavaScript")` 与 `testFramework(TestFrameworkType.Plugin.JavaScript)` 加载 JS 插件，
  均失败：测试环境插件解析日志显示
  `module intellij.platform.smRunner (namespace=jetbrains) is not resolved → intellij.javascript.testing excluded → JavaScript plugin excluded`。
  这是本机测试环境的模块解析问题（与既有 IJPL-248701 workaround 同类），非设计问题。
- 因此 S3/S4/S5 的**单元测试断言无法在本环境跑**；由 §2.3 的机制证据 + 官方插件同架构佐证
  （官方插件即以此架构提供生产级 HTML/JS 导航），判定在真实 IDE（runIde，JS 插件已加载）中成立。

### 回归：现有 58 个测试全部通过

`./gradlew test`（排除 spike 类）：68 个测试，10 个失败全部来自 `FtlSpikeHtmlTest`（JS 缺失所致），
其余 **58 个既有测试全绿**（FtlParserTest/FtlReferenceTest/FtlStructureViewTest/FtlFindUsagesTest/
FtlNamesValidatorTest/FreemarkerLexerTest/FreemarkerSyntaxHighlighterTest）。既有测试仅需把
`FtlElementTypes.TEMPLATE_DATA` 引用改名为 `TEMPLATE_TEXT`（两文件模型下 FTL 树数据区仍是
`TEMPLATE_TEXT` 叶节点，语义不变）。

## 4. Go/No-Go 决策：**Go（有条件）**

- S2（核心架构迁移）**已验证可行**且与现有测试兼容；
- S3/S4/S5 机制上成立（language-keyed + 全局 EP，非 file-type 门控），风险低；
- **条件**：正式实施的验收必须包含 runIde 人工验证（§9 已有）：`onclick="login()"` Ctrl+B 跳到
  同文件 `<script>` 的 `function login`、外部 `app.js`、`<script src>` 文件跳转。

## 5. 对正式实施计划的修正（相对原计划）

1. **无需改生成文件**：两文件模型不把 chameleon 放进 FTL 语法，`FtlElementTypes.java` 无需后处理；
   只需 BNF/flex 的 token 改名 + 重新生成（`generateParser generateLexer`）。
2. **FtlFileViewProvider 按官方两文件模型实现**（原计划写的是「保持单根 + chameleon 进 FTL 树」，
   Spike 证明新 API 不支持该路径——`this` 不能传入 super 构造、chameleon 是 IFileElementType 只能作文件根）。
3. **新增 `FtlFileElementTypes`**（OUTER_ELEMENT_TYPE + TEMPLATE_DATA），替代原计划的
   「FtlTemplateDataElementType.kt」。
4. **测试环境限制**：JS 插件因 `intellij.platform.smRunner` 未解析而无法在单元测试加载（本机环境问题）。
   正式阶段可选做：修复 smRunner 解析后启用 JS 相关单测；否则 JS 场景测试放到 runIde 手动验收。
5. **既有测试改动收敛为**：`FtlParserTest` 中 `TEMPLATE_DATA` → `TEMPLATE_TEXT`（3 处），语义不变。
6. 原计划 Task 1 的「生成文件覆盖问题（幂等后处理）」**取消**；Task 2/3/4 其余内容不变
   （HTML 验证、JS 导航、示例/文档/打包），其中 Task 3 的 JS 断言改为 runIde 验收。

## 6. Spike 遗留物

- 原型代码保留在工作区（`FtlFileViewProvider.kt`、`FtlFileElementTypes.kt`、flex/bnf 改名、
  `FtlParserTest` 改名、`FtlSpikeHtmlTest.kt`），正式实施在此基础上收尾；
- `.spike/`（反编译产物与原型资料）为临时目录，不入库；
- `build.gradle.kts` 已还原（不残留 JS 测试依赖）。
