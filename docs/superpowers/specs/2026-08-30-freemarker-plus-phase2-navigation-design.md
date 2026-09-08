# Freemarker Plus — Phase 2 代码导航设计

- 日期：2026-08-30
- 状态：待评审
- 目标 IDE：IntelliJ IDEA 2026.2（build 262），本机参考版本 2026.2.1（build 262.9437.185）

---

## 1. 背景与目标

Phase 1 已实现 `.ftl` 语法高亮（手写 `FreemarkerLexer` + `LayeredLexer` 叠加 HTML/CSS/JS），但**没有 PSI 树**，因此没有代码导航能力。

Phase 2 目标：为 `.ftl` 文件提供**代码导航（Code Navigation）**能力，即：

> 在 FreeMarker 模板中能够「跳转到定义 / 查找引用 / 查看结构 / 折叠 / 面包屑 / 重命名」。

具体用户可见功能：

- **跳转到声明（Ctrl+B / Ctrl+点击）**：`#include`/`#import` 的文件路径、`<@macro>` 调用、`${变量}` 引用、`ns.xxx` 命名空间引用
- **查找引用（Alt+F7）**：宏/函数/变量的所有使用处
- **结构视图（Alt+7）**：文件内的指令、宏、include 等结构
- **代码折叠**：`<#if>/<#list>/<#macro>/<#function>` 等块级指令折叠
- **面包屑导航**：光标所在指令/宏的上下文
- **重命名（Shift+F6）**：宏/函数/变量/命名空间的重命名

---

## 2. 官方插件调研结论

官方 FreeMarker 插件（plugin id `com.intellij.freemarker`）随 IntelliJ **Ultimate** 内置，版本 `262.9437.185`；其源码不在开源 `JetBrains/intellij-plugins` 仓库中（闭源）。本调研通过**反编译**本机 Gradle 缓存中 `idea-2026.2` 发行版自带的 `plugins/freemarker/lib/freemarker.jar`（`META-INF/plugin.xml` 与关键类签名）得到权威结论，非猜测。

### 2.1 官方插件的架构事实

| 事实 | 依据（类签名） |
|---|---|
| FTL 是模板语言 | `FtlLanguage extends Language implements TemplateLanguage` |
| `.ftl` 是模板文件类型 | `FtlFileType extends LanguageFileType implements TemplateLanguageFileType` |
| 使用模板语言 FileViewProvider（HTML 数据 + FTL 模板层） | `FtlFileViewProviderFactory implements FileViewProviderFactory` |
| PSI 解析器由 GrammarKit 生成 | `FtlParserDefinition implements ParserDefinition`、`_FtlLexer`、`FtlParser` |
| 引用通过 `PsiReferenceContributor` 注册 | `FtlReferenceContributor extends PsiReferenceContributor` |

> 结论：官方插件采用 **`TemplateLanguage`（模板语言）+ GrammarKit 解析器 + `TemplateLanguageFileViewProvider`** 的标准 IntelliJ 模板语言栈。这正是本插件 Phase 2 应采用的路线（Phase 1 的语言/文件类型已按 `TemplateLanguage` 方向搭建，天然对齐）。

### 2.2 官方插件导航相关扩展点 → 本插件 Phase 2 映射

| 官方扩展点 | 官方实现类 | 功能 | Phase 2 |
|---|---|---|---|
| `lang.parserDefinition` (FTL) | `FtlParserDefinition` | PSI 解析器（GrammarKit） | ✅ 核心地基 |
| `lang.fileViewProviderFactory` (FTL) | `FtlFileViewProviderFactory` | 模板语言 FileViewProvider | ❌（本方案用扁平 PSI，不需要，见 §4.2） |
| `psi.referenceContributor` (FTL) | `FtlReferenceContributor` | 引用提供者（文件/宏/变量/限定名） | ✅ 核心 |
| `lang.elementManipulator` (`FtlStringLiteral`) | `FtlStringManipulator` | 字符串字面量引用/重命名 | ✅ |
| `lang.findUsagesProvider` (FTL) | `FtlFindUsagesProvider` | 查找引用 | ✅ |
| `findUsagesHandlerFactory` | `FtlFindUsagesHandlerFactory` | 查找引用处理器 | ✅ |
| `definitionsSearch` | `FtlDefinitionSearcher` | 定义搜索（跨文件） | ✅ |
| `referencesSearch` | `FtlMethodUsageSearcher` | 引用搜索（跨文件） | ✅（修订 2026-09-08：实现注册的是通用 `referencesSearch`，非原计划的 `methodReferencesSearch`） |
| `lang.psiStructureViewFactory` (FTL) | `FtlStructureViewBuilderProvider` | 结构视图 | ✅ |
| `lang.foldingBuilder` (FTL) | `FtlFoldingBuilder` | 代码折叠 | ✅ |
| `breadcrumbsInfoProvider` | `FtlBreadcrumbsInfoProvider` | 面包屑 | ✅ |
| `renamePsiElementProcessor` | `FtlRenameProcessor` | 重命名 | ✅ |
| `renameHandler` | `FtlPropertyRenameHandler` | 属性重命名 | ❌（修订 2026-09-08：未实现——声明重命名已由 `FtlIdentifierMixin`（`PsiNameIdentifierOwner`）+ 平台默认 rename 流程覆盖，`FtlRenameProcessor` 仅负责拒绝 import 别名改名） |
| `fileBasedIndex` | `FtlFileIndex` | 文件索引（宏/变量声明） | ✅（跨文件导航） |
| `gotoTargetRendererProvider` | `FtlElementCellRenderer` | 跳转目标渲染 | ⭕ 可选 |
| `lang.elementManipulator` (`FtlMacro`) | `FtlMacroManipulator` | 宏操作 | ⭕ 可选 |
| `customPropertyScopeProvider` | `FtlPropertyScopeProvider` | 属性作用域 | ❌ 暂缓 |
| `lang.parserDefinition` (FTL) | `FtlSquareParserDefinition` | 方括号 `[#..]` 方言 | ❌ 不做 |
| `completion.contributor` | `FtlCompletionContributor` | 代码补全 | ❌ Phase 3 |
| `localInspection` (References/Types/Calls…) | `Ftl*Inspection` | 语义检查（未解析引用红色波浪线等） | ❌ Phase 3 |
| `lang.documentationProvider` | `FtlDocumentationProvider` | 文档 | ❌ Phase 3 |
| `codeInsight.parameterInfo` | `FtlParameterInfoHandler` | 参数提示 | ❌ Phase 3 |
| `lang.formatter` | `FtlFormattingModelBuilder` | 格式化 | ❌ Phase 3 |

### 2.3 官方插件的引用类型与解析目标

官方 `psi/directives/` 与 `psi/variables/` 下的类（反编译文件名）揭示了完整的「引用 → 目标」图谱，本插件 Phase 2 据此裁剪：

| 引用类型 | 触发语法 | 解析目标（声明） | 官方类 |
|---|---|---|---|
| 文件引用 | `<#include "a.ftl">`、`<#import "a.ftl" as ns>` | 目标 `.ftl` 文件 | `FtlFileReference`、`FtlFileReferenceDirective`、`JarAwareFileReference` |
| 宏调用引用 | `<@foo ...>` | `<#macro foo>` | `FtlMacro` |
| 函数调用引用 | `foo(...)` | `<#function foo>` | `FtlSignatureDirective` |
| 变量引用 | `${foo}`、`foo.bar` 的首段 | `<#assign>`/`<#local>`/`<#global>`/`<#list as foo>`/宏参数 | `FtlVariable` 系列 |
| 命名空间引用 | `ns.foo` | 被 `#import` 文件的宏/变量 | `FtlQualifiedReference` |
| 枚举引用 | `ClassName.ENUM`（Ultimate 数据模型） | Java 枚举常量 | `FtlEnumReference` |

> Phase 2 只做「FTL 内部 + 文件系统」的导航（上表除「枚举引用 / Java 数据模型」外）。Java 数据模型（`FtlDataModelVariable` → Java 类/方法/字段）依赖 `java.psi` 与 Ultimate，属 Phase 3+。

---

## 3. 范围

### 3.1 本阶段包含

- PSI 地基：GrammarKit 语法 + 生成的 `FtlParser`/`FtlLexer` + `FtlParserDefinition` + `FtlFile` + 元素类型
- 扁平 FTL PSI：整份 `.ftl` 由 FTL 解析器解析，HTML/CSS/JS 数据区为不透明的 `TEMPLATE_DATA` 叶节点（不叠加 HTML PSI，见 §4.2）
- 引用与跳转：文件引用、宏/函数引用、变量引用、命名空间引用（同文件 + 跨文件）
- 查找引用、结构视图、折叠、面包屑、重命名
- 跨文件宏/变量声明的文件级索引
- 自动化测试（引用解析、结构视图、折叠的轻量测试）

### 3.2 本阶段明确不做（留待 Phase 3+）

- 代码补全（Ctrl+Space）、参数提示
- 语义检查（未解析引用红色波浪线、类型检查、内建函数检查）——注：跳转本身能 work，但「找不到就标红」的 inspection 留到 Phase 3
- 格式化（`lang.formatter`）、注释器（`lang.commenter`）
- Java 数据模型解析（`FtlDataModelVariable` → Java 类/方法/字段）、Spring/Web/i18n 集成
- 方括号 `[#..]` 方言（`FtlSquareParserDefinition`）

---

## 4. 技术路线决策

### 4.1 采用 GrammarKit 生成解析器

- 插件：`org.jetbrains.intellij.platform.grammarkit`（IPGP 2.x 官方语法套件，已与 IPGP 合并维护）。
- 理由：官方插件即用 GrammarKit；生成式解析器自带**错误恢复**与**增量重解析**，且是补全/引用/结构视图的标准 PSI 入口；手写递归下降维护成本高、错误恢复差。
- BNF 语法见 §6。

### 4.2 采用「扁平 FTL PSI」（HTML 数据区为不透明叶节点）

- 官方插件用 `TemplateLanguageFileViewProvider` 把数据区交给平台 HTML 解析器（其 `TEMPLATE_DATA` 是自定义 `TemplateDataElementType`，通过 `LookAheadLexer` 包装实现——已通过反编译 `FtlFileElementTypes`/`FtlLexer` 证实）。该机制的收益是 **HTML 侧的导航**，代价是模板语言分层机制的复杂集成（模板数据元素类型 + lexer 合并 + FileViewProvider）。
- **本阶段决策**：`.ftl` 整体由 FTL 解析器解析，HTML/CSS/JS 数据区成为**不透明的 `TEMPLATE_DATA` 叶节点**（普通 `FtlElementType`），不叠加 HTML PSI。
  - 理由：Phase 2 目标是 **FTL 代码导航**（FTL 声明跳转/查找引用/结构/折叠/面包屑），全部可在扁平 FTL PSI 上完成；HTML 导航不是 Phase 2 目标，避免引入最大技术风险点。
  - Phase 1 高亮器（`FreemarkerSyntaxHighlighter` + 手写 lexer）独立于 PSI，完全不受影响。
  - 未来若需 HTML 导航，可迁移到官方同款模板语言机制，届时 FTL PSI 结构保持不变（仅数据区处理方式变化）。

### 4.3 保留 Phase 1 高亮，新增 PSI 高亮兜底

- 保留现有 `lang.syntaxHighlighterFactory`（`FreemarkerSyntaxHighlighter`）不动，避免破坏 Phase 1 的高亮。
- `editorHighlighterProvider`（`FtlEditorHighlighterProvider`）——未实现（Phase 2 未做；高亮继续走 Phase 1 的 `lang.syntaxHighlighterFactory`）。
- 若二者存在视觉差异，以 Phase 1 的配色页（`FreemarkerColorSettingsPage`）为准，逐步对齐。

---

## 5. 架构与组件

语言 ID：`FreemarkerPlus`（修订 2026-09-08：Phase 1/2 原规划为 `FTL`，实现时调整为 `FreemarkerPlus`，避免与官方插件语言 ID 冲突，见 commit `95845ef`）。

| 组件 | 基类/接口 | 职责 |
|---|---|---|
| `FtlElementTypes` / `FtlTokenTypes` | `IElementType`/`IFileElementType` 集合 | PSI 元素与 token 类型定义 |
| `Freemarker.bnf` | GrammarKit 语法 | 声明 FTL 文法 → 生成 `FtlParser` + `FtlLexer` |
| `FtlParserDefinition` | `ParserDefinition` | 连接 lexer/parser/文件节点 |
| `FtlFile` | `PsiFileBase` | FTL PSI 文件根，提供声明查找/隐式变量等辅助 |
| `FreemarkerFileType` | `LanguageFileType` | 沿用 Phase 1，无需改动 |
| `FtlIncludeDirective`/`FtlImportDirective`/`FtlMacro`/`FtlFunction`/`FtlAssignDirective`/`FtlListDirective` 等 | `PsiElement` 子类 | 各类指令的 PSI 元素（提供声明/引用） |
| `FtlStringLiteral` | `PsiElement` + `ContributedReferenceHost` | 字符串字面量（承载文件引用） |
| `FtlFileReference` | `PsiReferenceBase<PsiElement>` | `#include`/`#import` 文件路径引用 |
| `FtlMacroReference`/`FtlVariableReference`/`FtlNamespaceReference` | `PsiReferenceBase<PsiElement>` | 宏/变量/命名空间引用 |
| `FtlReferenceContributor` | `PsiReferenceContributor` | 按 pattern 注册各类引用 |
| `FtlFindUsagesProvider` | `FindUsagesProvider` | 查找引用的文字/类型描述 |
| `FtlFindUsagesHandlerFactory` | `FindUsagesHandlerFactory` | 查找引用处理器 |
| `FtlDefinitionSearcher` / `FtlMethodUsageSearcher` | `QueryExecutorBase` | 跨文件定义/引用搜索 |
| `FtlFileIndex` | `FileBasedIndexExtension` | 索引每个文件的宏/函数/变量声明 |
| `FtlStructureViewBuilderProvider` / `FtlStructureViewModel` | `PsiStructureViewFactory` / `TextEditorBasedStructureViewModel` | 结构视图 |
| `FtlFoldingBuilder` | `FoldingBuilder` | 块级指令折叠 |
| `FtlBreadcrumbsInfoProvider` | `BreadcrumbsProvider` | 面包屑 |
| `FtlRenameProcessor` | `RenamePsiElementProcessor` | 重命名 |
| `FtlStringManipulator` | `ElementManipulator<FtlStringLiteral>` | 字符串字面量的引用文本/替换 |

### 5.1 数据流

```
打开 .ftl
  → FreemarkerFileType（Phase 1，不变）
  → FtlParserDefinition 解析整份文件为扁平 FTL PSI：
      FtlFile
      ├─ TEMPLATE_DATA 叶节点（HTML/CSS/JS 数据区，不透明）
      └─ FTL 节点：指令 / 宏调用 / 插值 / 注释
  → Ctrl+B / Alt+F7 / Alt+7 / 折叠 / 面包屑 / Shift+F6
      均由 FTL PSI 上的引用(Reference) 与 声明(Declaration) 驱动
```

---

## 6. PSI 语法设计（BNF 概览）

GrammarKit 语法目标：**能承载导航所需的结构**，不必完整覆盖 FreeMarker 表达式语义（完整表达式类型系统属 Phase 3）。

```
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

> 说明：
> - `TEMPLATE_DATA` 是普通 `FtlElementType`（不透明叶节点），由 lexer 的正则 token 对 HTML 区域输出；不叠加 HTML PSI（见 §4.2）。
> - 表达式只解析到「限定名」粒度，足以支撑 `${user.name}` 的变量/属性导航；完整运算符/类型在 Phase 3 扩充。
> - 指令体（`<#if>...</#if>` 的内容）在语法上由 `outer_element*` 自然承载，无需单独展开 `if_body` 等规则；折叠/结构视图基于 PSI 树的指令节点配对完成。

---

## 7. 引用与解析设计

### 7.1 引用注册（`FtlReferenceContributor`）

按 pattern 匹配 PSI 元素并附上引用，而非在元素上硬编码：

| 宿主元素 | 引用类 | 触发条件 |
|---|---|---|
| `FtlIncludeDirective` / `FtlImportDirective` 内的 `FtlStringLiteral` | `FtlFileReference` | 指令名为 `include`/`import`，字符串是第一个参数 |
| `FtlMacro` 的宏名标识符（`<@name>`） | `FtlMacroReference` | 宏调用名 |
| `INTERPOLATION`/`expression` 内的首段 `IDENTIFIER` | `FtlVariableReference` | 变量引用 |
| 限定名 `a.b` 的 `a`（当 `a` 是已 import 的命名空间） | `FtlNamespaceReference` | 命名空间引用 |

### 7.2 解析策略

- **同文件**：向上/向下遍历 PSI 树，收集可见声明（`#macro`/`#function`/`#assign`/`#local`/`#global`/`#list as`/宏参数），按作用域规则匹配。
- **跨文件**：通过 `FtlFileIndex`（§10）查全项目已索引的宏/函数/变量声明；`#import "lib.ftl" as ns` 的命名空间引用 `ns.foo` 解析到 `lib.ftl` 内名为 `foo` 的宏/变量。
- **文件引用**：`FtlFileReference` 复用平台 `FileReferenceSet`，解析 `#include`/`#import` 的字符串为项目内 `.ftl` 文件（支持相对路径、jar 内路径由平台处理）。

### 7.3 声明元素（可作为跳转目标）

| 声明语法 | PSI 元素 | 名字元素 |
|---|---|---|
| `<#macro foo ...>` | `FtlMacro` | `foo` |
| `<#function foo ...>` | `FtlFunction` | `foo` |
| `<#assign foo = ...>` / `<#local>` / `<#global>` | `FtlAssignDirective` | `foo` |
| `<#list items as foo>` | `FtlListDirective` | `foo` |
| `<#import "x" as ns>` | `FtlImportDirective` | `ns` |

---

## 8. 结构视图 / 折叠 / 面包屑

- **结构视图**（`FtlStructureViewModel`）：列出「指令」节点（扁平列表，嵌套树留待后续语法增强；`#if`/`#list`/`#macro`/`#function`/`#assign`/`#include`/`#import` 等），图标/文本取自指令名 + 摘要。数据区（HTML）为不透明叶节点，不进入结构视图。
- **折叠**（`FtlFoldingBuilder`）：对成对的块级指令（`<#if>/</#if>`、`<#list>/</#list>`、`<#macro>/</#macro>`、`<#function>/</#function>`、`<#switch>/</#switch>` 等）生成 `FoldingDescriptor`，占位文本为 `<#if>` 或 `<#if condition>`。
- **面包屑**（`FtlBreadcrumbsInfoProvider`）：`acceptElement` 接受指令/宏节点，`getElementInfo` 返回 `<#list items>` 之类的文本，`getParent` 返回 `element.parent`（扁平 PSI 下的直接父节点；嵌套层级树留待后续语法增强）。

---

## 9. 查找引用 / 重命名

- **查找引用**：
  - `FtlFindUsagesProvider`：对宏/变量/命名空间等提供 `getType`（`Macro`/`Function`/`Variable`/`Namespace`）与 `getDescriptiveName`。
  - `FtlFindUsagesHandlerFactory`：为 `FtlFile`/声明元素创建 handler；`FtlDefinitionSearcher` 与 `FtlMethodUsageSearcher` 负责「找定义」与「找方法引用」的跨文件实现（基于 `FtlFileIndex`）。
- **重命名**：
  - `FtlRenameProcessor` 处理宏/变量/命名空间的 `RenamePsiElementProcessor`，随查找引用结果一并更新所有使用处。
  - `FtlStringManipulator` 处理字符串字面量内路径/名字的重命名。

---

## 10. 跨文件导航（文件级索引）

`FtlFileIndex extends FileBasedIndexExtension<String, List<FtlIndexInfo>>`：

- **键**：宏/函数/变量的名字（字符串）。
- **值**：声明所在文件 + 声明 PSI 文本范围 + 类型（macro/function/variable）。
- **构建**：在 `getIndexer` 中遍历 `FtlFile` 的顶层声明（复用 `FtlDefinitionSearcher` 的收集逻辑）。
- **消费**：`FtlNamespaceReference`/跨文件宏引用解析、`FtlDefinitionSearcher`、补全（Phase 3）共用。

---

## 11. 插件注册（plugin.xml 增量）

在 Phase 1 的 `plugin.xml` 上新增（全部 `defaultExtensionNs="com.intellij"`）：

```xml
<lang.parserDefinition language="FreemarkerPlus"
                       implementationClass="com.freemarkerplus.psi.FtlParserDefinition"/>
<psi.referenceContributor language="FreemarkerPlus"
                          implementation="com.freemarkerplus.reference.FtlReferenceContributor"/>
<lang.findUsagesProvider language="FreemarkerPlus"
                         implementationClass="com.freemarkerplus.navigation.FtlFindUsagesProvider"/>
<findUsagesHandlerFactory implementation="com.freemarkerplus.navigation.FtlFindUsagesHandlerFactory"/>
<definitionsSearch implementation="com.freemarkerplus.navigation.FtlDefinitionSearcher"/>
<referencesSearch implementation="com.freemarkerplus.navigation.FtlMethodUsageSearcher"/>
<lang.psiStructureViewFactory language="FreemarkerPlus"
                              implementationClass="com.freemarkerplus.navigation.FtlStructureViewBuilderProvider"/>
<lang.foldingBuilder language="FreemarkerPlus"
                     implementationClass="com.freemarkerplus.navigation.FtlFoldingBuilder"/>
<breadcrumbsInfoProvider implementation="com.freemarkerplus.navigation.FtlBreadcrumbsInfoProvider"/>
<renamePsiElementProcessor implementation="com.freemarkerplus.navigation.FtlRenameProcessor"/>
<lang.elementManipulator forClass="com.freemarkerplus.psi.FtlStringLiteral"
                         implementationClass="com.freemarkerplus.reference.FtlStringManipulator"/>
<fileBasedIndex implementation="com.freemarkerplus.reference.FtlFileIndex"/>
```

> `fileType`（`FreemarkerFileType`）沿用 Phase 1，无需改动（语言与扩展名不变）。
>
> 修订 2026-09-08：以上片段已按实现对齐（`language="FreemarkerPlus"`、`referencesSearch`）；实现另注册了 `lang.namesValidator`（`FtlNamesValidator`）与 `lang.elementManipulator`（`FtlIdentifier` / `FtlIdentifierManipulator`）。完整清单以 `src/main/resources/META-INF/plugin.xml` 为准。

---

## 12. 测试策略

- **Lexer/Parser 单元测试**：喂典型 `.ftl` 片段，断言生成的 PSI 树结构（指令类型、名字文本、嵌套关系）。
- **引用解析测试**：`PsiTestUtil`/轻量 fixture 下，`assertResolvesTo` 验证 `#include`→文件、`<@m>`→`<#macro m>`、`${v}`→`<#assign v>`、`ns.m`→被 import 文件的宏。
- **结构视图/折叠测试**：断言 `FtlStructureViewModel` 树形、`FtlFoldingBuilder` 折叠区间。
- **查找引用/重命名测试**：`CodeInsightTestFixture` 的 `testFindUsages` / `testRename`。
- 测试数据置于 `src/test/resources/testData/`，跨文件用例用多个临时文件。

---

## 13. 未来阶段预留

- 表达式完整语法 + 类型系统（`FtlType` 系列）→ 补全、类型检查、参数提示
- 语义检查 inspections（未解析引用/类型/调用错误）
- Java 数据模型解析（`FtlDataModelVariable`）、Spring/Web/i18n 集成
- 格式化、注释器、文档 provider
- 方括号 `[#..]` 方言

---

## 14. 修订记录

### 2026-09-08 实现对齐修订（对照 `src/main/resources/META-INF/plugin.xml` 与实际代码）

1. **语言 ID**：规划为 `FTL` → 实现为 `FreemarkerPlus`（避免与官方插件语言 ID 冲突，commit `95845ef`）。§5 与 §11 片段已同步更正。
2. **§2.2 / §11 引用搜索扩展点**：实现注册的是通用 `referencesSearch`（`FtlMethodUsageSearcher`），非规划中的 `methodReferencesSearch`。
3. **§2.2 `renameHandler`**：未实现。声明重命名由 `FtlIdentifierMixin`（`PsiNameIdentifierOwner`）+ 平台默认 rename 流程覆盖，`FtlRenameProcessor` 仅负责拒绝 import 别名改名。
4. **§11 片段与实际注册的差异**：实现额外注册了 `lang.namesValidator` 与 `lang.elementManipulator`（`FtlIdentifier`）。
5. **§4.2「扁平 PSI」决策已被 Phase 2.5 取代**：已迁移至官方模板语言机制的双文件 view provider（数据区为真实 HTML/XML PSI，chameleon 懒展开），见 `docs/superpowers/plans/2026-08-30-freemarker-plus-phase25-spike-notes.md` 与 commit `03e20a4`。
