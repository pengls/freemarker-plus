# Freemarker Plus — Phase 1 语法高亮设计

- 日期：2026-08-30
- 状态：待评审
- 目标 IDE：IntelliJ IDEA（Community 与 Ultimate 通用），平台构建号 262（2026.2），本机参考版本 2026.2.1（build 262.9437.185）

---

## 1. 背景与目标

开发一款用于**替代官方 Freemarker 插件**的 IntelliJ 插件。Phase 1 只聚焦一件事：

> 让 `.ftl` 文件拥有正确的**语法高亮**——识别 HTML / CSS / JS，以及 Freemarker 语法。

具体颜色诉求（作为**默认配色**，主题感知、用户可自定义）：

- HTML 元素标签：黄色系
- 单/双引号包裹的字符串：绿色系
- 注释：灰色系

> 注：以上颜色是"默认色"，走 IntelliJ 颜色方案（Color Scheme）体系，随深色/浅色主题自动适配，用户可在 Settings → Editor → Color Scheme → Freemarker 中覆盖。**不硬编码颜色。**

---

## 2. 范围

### 2.1 本阶段包含

- `.ftl` 文件类型注册与 Freemarker 语言定义
- Freemarker 语法高亮：插值 `${...}`、指令 `<#...>`、宏 `<@...>`、注释 `<#-- -->`、表达式内字符串/数字/关键字/操作符
- HTML / CSS / JS 区域的高亮（复用平台内置能力）
- 自定义配色方案页（`ColorSettingsPage`）与默认色
- Lexer 与高亮的自动化测试

### 2.2 本阶段明确不做（留待 Phase 2+）

- PSI 解析器 → 因此**不含**：代码补全、定义/引用跳转、折叠、结构视图、语义级错误提示、格式化、重构
- Freemarker 语义/类型检查
- 仅注册 `.ftl`（`.ftlh`/`.ftlx` 后续一行配置即可增加）

---

## 3. 技术路线决策

采用 **Template Language 叠加（Template Language Overlay）** 机制，而非自行实现 HTML/CSS/JS 词法分析，也非 TextMate 语法包。

理由：

1. HTML / CSS / JS 的识别、高亮、未来补全由 IDE **内置插件**免费提供，无需重写。
2. 官方 Freemarker 插件及 Velocity/Thymeleaf 插件均采用此机制，是可持续的扩展地基。
3. 颜色精确可控、主题感知，满足"黄/绿/灰"需求。
4. 外部库（ANTLR 等）与 IntelliJ PSI/高亮体系结合别扭，属反模式。

关键点：我们**只写 Freemarker 部分的 Lexer**，其职责是切分"Freemarker 区域 vs 数据区域"，不需要理解 HTML/CSS/JS 细节。

---

## 4. 架构与组件

| 组件 | 类型/基类 | 职责 |
|---|---|---|
| `FreemarkerLanguage` | `Language` + `TemplateLanguage` | 声明 Freemarker 为模板语言 |
| `FreemarkerFileType` | `LanguageFileType` + `TemplateFileType` | `.ftl` 扩展名 → Freemarker 语言 |
| `FreemarkerTokenType` | `IElementType` 集合 | Freemarker 各类 token 定义 |
| `FreemarkerLexer` | `Lexer`（手写状态机） | 切分 Freemarker 区域与数据区域，子 token 化 Freemarker 表达式 |
| `FreemarkerSyntaxHighlighter` | `DefaultTemplateHighlighter` | token → 颜色键映射；数据区域转交 HTML/CSS/JS |
| `FreemarkerSyntaxHighlighterFactory` | `SyntaxHighlighterFactory` | 工厂，注册到扩展点 |
| `FreemarkerColorSettingsPage` | `ColorSettingsPage` | 默认色、设置页分组、预览样例 |

语言 ID：`FTL`（与官方插件一致，便于用户认知迁移）。

### 4.1 数据流

```
打开 .ftl 文件
  → FileTypeManager 解析为 FreemarkerFileType（TemplateFileType）
  → 编辑器请求高亮 → FreemarkerSyntaxHighlighterFactory
  → DefaultTemplateHighlighter(FreemarkerLexer)
  → FreemarkerLexer 输出：
       TEMPLATE_DATA（HTML/CSS/JS 区域）→ 平台 HTML 高亮器二次处理（含 <style>/<script> 内 CSS/JS）
       FTL_* token                        → 映射到 TextAttributesKey → 主题解析 → 绘制
  → 合并绘制
```

---

## 5. 词法分析器（FreemarkerLexer）

手写、全函数（任何输入都不抛异常）、有状态。

### 5.1 状态切换

- **数据区（默认）**：扫描直到遇到 Freemarker 定界符，期间输出 `TemplateDataElementType.TEMPLATE_DATA`。
- **Freemarker 表达式 `${...}`**：进入后子 token 化内部。
- **Freemarker 标签 `<#...>` / `</#...>` / `<@...>`**：进入后子 token 化标签名与参数。
- **Freemarker 注释 `<#-- ... -->`**：整体一个 `FTL_COMMENT` token。

### 5.2 定界符识别

- `${` 开头、`}` 结束 → 插值
- `<#--` 开头、`-->` 结束 → 注释
- `<#` / `</#` 开头、`>` 结束 → 指令标签
- `<@` 开头、`>` 结束 → 宏调用标签
- `\${` → 转义，按字面文本（数据区）处理，不进入 Freemarker

### 5.3 Token 粒度与默认色

| Token | 覆盖内容 | 默认色 |
|---|---|---|
| `FTL_COMMENT` | `<#-- ... -->` | 灰 |
| `FTL_STRING` | 表达式内单/双引号字符串 | 绿 |
| `FTL_KEYWORD` | `if` `else` `elseif` `list` `include` `assign` `macro` `function` 等 | 关键字色 |
| `FTL_DIRECTIVE_NAME` | 标签名（如 `#if`、`@myMacro` 的名字部分） | 指令色 |
| `FTL_INTERPOLATION` | `${` `}` `<#` `</#` `<@` `>` 等定界符 | 括号色 |
| `FTL_IDENTIFIER` | 变量名 / 表达式标识符 | 默认前景 |
| `FTL_NUMBER` | 数字字面量 | 数字色 |
| `FTL_OPERATOR` | 运算符（`==` `&&` `+` `!` 等） | 操作符色 |
| `FTL_BAD_CHARACTER` | 未闭合 / 非法输入 | 红 |
| `TEMPLATE_DATA` | 数据区域（HTML/CSS/JS） | 交由平台高亮 |

> 说明："黄/绿/灰"诉求的归属：
> - HTML 标签 / HTML 属性值字符串 / JS 字符串 / CSS 字符串 / HTML 注释 → **平台默认配色**（默认主题下标签偏黄、字符串绿、注释灰，且主题感知）。
> - Freemarker 字符串（绿）、Freemarker 注释（灰）、指令/关键字 → **本插件的 `ColorSettingsPage` 默认色**。
>
> 精确十六进制色值在实现阶段对照默认配色微调，以保证与平台默认观感一致。

---

## 6. 语法高亮器

`FreemarkerSyntaxHighlighter extends DefaultTemplateHighlighter`：

- 构造时传入 `FreemarkerLexer`。
- `getTokenHighlights(tokenType)`：
  - `TEMPLATE_DATA` → 交给数据语言（HTML）高亮器处理（平台机制）。
  - `FTL_*` → 映射到对应 `TextAttributesKey`。

`FreemarkerSyntaxHighlighterFactory` 注册到 `lang.syntaxHighlighterFactory`。

---

## 7. 配色方案（FreemarkerColorSettingsPage）

- `getAttributeDescriptors()`：按组返回 `AttributesDescriptor`（注释、字符串、关键字、指令、定界符、数字、操作符、坏字符）。
- `getDemoText()`：提供一段典型 `.ftl` 样例（HTML + `<style>`/`<script>` + Freemarker 指令/插值/注释）作为设置页预览。
- 通过默认属性键提供**默认色**（黄/绿/灰等），用户可在设置页覆盖。

---

## 8. 插件注册（plugin.xml）

- 基础信息：id、name、version、sinceBuild=`262`、untilBuild=`262.*`。
- 依赖：`com.intellij.modules.platform`、`com.intellij.modules.lang`（HTML/CSS/JS 为 IDE 内置，运行时经数据语言机制使用）。
- 扩展点：
  - `fileType` / 文件类型注册（FreemarkerFileType）
  - `lang.syntaxHighlighterFactory`（FreemarkerSyntaxHighlighterFactory）
  - `colorSettingsPage`（FreemarkerColorSettingsPage）

---

## 9. 构建与工具链

- 构建脚本：Gradle Kotlin DSL；源码：**Kotlin**。
- IntelliJ Platform Gradle Plugin **2.x**，`platformVersion = "2026.2"`。
- JDK：**21+**（2026.2 平台运行时为 Java 25；构建 JDK 具体版本在实现时钉死，确保与 IPGP 兼容）。
- Gradle wrapper 版本在实现时钉死（与 IPGP 2.x 兼容的版本）。
- 产物：`.zip` 分发插件，可 `runIde` 直接在本机 2026.2.1 上调试。

---

## 10. 错误处理

- Lexer 为全函数：非法输入一律返回合法 token（回退为数据区文本或 `FTL_BAD_CHARACTER`），不抛异常、不导致编辑器异常。
- `\${` 转义 → 字面文本。
- 未闭合 `${` / `<#...` → `FTL_BAD_CHARACTER` 标红。
- HTML/CSS/JS 区域自身异常由平台高亮器兜底，不影响本插件。

---

## 11. 测试

- **Lexer 单元测试**：喂样例字符串，断言 token 类型与起止偏移（覆盖插值、指令、宏、注释、转义、未闭合、混合 HTML/CSS/JS）。
- **高亮测试**：`HighlighterTestKit` 断言指定区间命中的 `TextAttributesKey`。
- 测试框架：IntelliJ 轻量测试 fixture（`LightPlatformTestCase`）。
- `testData/` 提供典型样例（含 CSS/JS 嵌入的页面）。

---

## 12. 未来阶段预留

- 在 Freemarker token 类型与配色键已按语义拆分的基座上，后续可平滑追加：
  - PSI 解析器 → 补全、跳转、折叠、结构视图、语义报错、格式化
  - `.ftlh` / `.ftlx` 文件类型
  - Freemarker 版本 / 数据语言按文件配置
