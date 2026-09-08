# Freemarker Plus 修复路线图（vs 官方 FreeMarker 插件差距清单）

> 依据 2026-09-08 的功能对比评估制定（官方插件 `com.intellij.freemarker` 仅支持 IDEA Ultimate，
> 本插件为其社区版平替）。评审结论与逐文件证据见 git 历史与 `docs/superpowers/` 下的 spec。
>
> **优先级规则**（项目维护者定）：
> 1. 优先服务「查看 FTL 代码逻辑」的能力：语法高亮识别、导航（跳转/查找/结构/折叠）；
> 2. 「写 FTL」相关能力（代码补全、错误提示、格式化等）降级；
> 3. 工作量小的顺手修复排前面。

状态图例：⬜ 未开始 ｜ 🔄 进行中 ｜ ✅ 已完成

---

## P0 · 速赢（每个 ≤ 半天，直接提升「看」的准确性）

| # | 状态 | 修复项 | 说明 | 工作量 |
|---|---|---|---|---|
| 1 | ✅ | **两套 lexer 一致性** | 高亮 `FreemarkerLexer` 补 `#{` 识别（legacy 插值高亮修复）；移除高亮侧 `\$`/`\<` 转义特判，两侧统一为「`\` 是普通文本」的 FreeMarker 语义 | S |
| 2 | ✅ | **import 多级路径解析** | `FtlNamespaceReference.findImportedFile` 改为复用 import 字符串字面量上的 `FtlFileReference`（`FileReferenceSet` 按 `/` 分段），`partials/lib.ftl` 多级 import 的 `ns.member` 跳转修复 | S |
| 3 | ✅ | **宏/函数参数纳入变量解析** | `FtlPsiUtil.findVariableDeclarations` 补收 macro/function 参数（位置参数与 `name=default` 均支持）→ 宏体内 `${param}` Ctrl+B / Find Usages / Rename 可用 | S |
| 4 | ✅ | **折叠块补齐** | `FOLDABLE_NAMES` 加 `compress`/`noparse`/`escape`；新增 `<#-- -->` 注释整体折叠（按 COMMENT_START/COMMENT_END 叶子配对，占位符「首行 … -->」） | S |
| 5 | ✅ | **Commenter（Ctrl+/）** | `lang.commenter` 注册 `FreemarkerCommenter`（块注释 `<#-- -->`）；编辑器动作行为已由 fixture 测试验证 | S |
| 6 | ✅ | **Find Usages 类型文案细分** | `FtlFindUsagesProvider.getType` 按父指令细分：Macro / Function / Namespace（import 别名）/ Loop variable（list as）/ Variable（assign 族） | S |

## P1 · 短期（每个 1~2 天，「查看代码逻辑」感知最强）

| # | 状态 | 修复项 | 说明 | 工作量 |
|---|---|---|---|---|
| 7 | ✅ | **`?` 内建函数解析** | TAG/插值态新增 `?`/`??` token；BNF 表达式扩展为 `primary (DOT primary \| ?identifier [args] \| ?? \| 比较运算)*`（保持扁平结构，`primaryList` 访问器不回退）。支持 `${list?size}`、`${x?string("a","b")}`、`${x?upper_case?length}`、`${(a>b)?string("y","n")}`、`<#if user??>` | M |
| 8 | ✅ | **括号内 `>` 截断修复** | 解析/高亮两套 lexer 均加括号深度计数：括号内 `>` `<` `>=` `<=` 为比较运算 token，括号外 `>` 照旧结束指令（与 FreeMarker 尖括号语法语义一致）。插值态比较运算无条件支持。同批顺带：generic 指令属性允许 `as`（`<#escape x as html>` 修复）、块级 `<#assign x>...</#assign>` 解析 + 折叠 | M |
| 9 | ⬜ | **结构视图层级化** | PSI 扁平导致 Alt+7 是平铺清单；用折叠同款栈配对把 if/list/switch/macro 建成嵌套树 + 节点图标 | M |
| 10 | ⬜ | **BraceMatcher 指令配对** | `<#if>`↔`</#if>` 配对高亮 + Ctrl+Shift+M 互跳 | M |
| 11 | ⬜ | **Gutter 图标** | 宏/函数声明行侧栏图标 + 用法行导航标记（LineMarkerProvider） | S~M |
| 追加 | ⬜ | **块级 `<#assign x>...</#assign>` 解析** | `assign_directive` 规则强制 `= expr`，块赋值形式解析报错（真实模板常见）；需 BNF 扩展 + 重新生成解析器，实施时随 #7/#8 词法语法批次一并做 | M |

## P2 · 偏大（按需启动，均为「读」服务的地基）

| # | 状态 | 修复项 | 说明 | 工作量 |
|---|---|---|---|---|
| 12 | ⬜ | 运算符全家桶（算术/比较/逻辑/`??`/`!`）+ 字符串内插值拆分 | 完整覆盖真实模板的阅读需求，依赖 #7 的词法地基 | M~L |
| 13 | ⬜ | `@ftlvariable` 最小可用 | 注释内容进 PSI，解析 `name` → 变量声明，数据模型变量 Ctrl+B 跳到注释处；不做 Java 类型解析 | M |
| 14 | ⬜ | FTL 文件改名联动 include/import 路径 | Rename 时同步改引用方路径字符串 | M |
| 15 | ⬜ | Template Data Languages 配置化 | 数据语言按扩展名硬编码 HTML/XML，增加设置页/按文件覆盖 | M~L |

## 明确缓做（按优先级规则降级）

代码补全、Inspection 标红/意图动作、格式化、Live Templates、参数信息、Java 数据模型
（Java 类/字段解析）、Spring/Web 集成、方括号 `[#..]` 方言、关键字名宏（`<@list>`）解析边缘 case。

---

## 决策记录

| 日期 | 决策 | 理由 |
|---|---|---|
| 2026-09-08 | P0-1 的 `\$` 统一方向选「移除高亮侧 `\$`/`\<` 转义」，而非给解析侧加转义 | FreeMarker 官方语义中模板文本的 `\` 不是转义符，`\${x}` 实际输出 `\` + 插值结果；解析侧现状（`\` 为文本、`${` 照常插值）才是对的，高亮侧应向它对齐 |
| 2026-09-08 | P0-3 只做同文件变量解析（`FtlPsiUtil`），不改 `FtlFileIndex` | 宏参数是调用期局部概念，跨文件索引参数只会制造噪音；跨文件声明导航仍只面向宏/函数/assign/list |
| 2026-09-08 | P0-4 注释折叠按 COMMENT_START/COMMENT_END **叶子配对**实现，而非 PSI 的 `COMMENT` 复合节点 | `FtlParserDefinition.getCommentTokens` 注册了 COMMENT_START/END，PsiBuilder 把注释拆成两个叶子（内容不进 PSI），`COMMENT` 规则节点在真实 PSI 中不存在 |
| 2026-09-08 | P0-4 暂不覆盖 `<#escape x as ...>` 的实际折叠收益 | generic 指令属性规则不认 `as` token，`<#escape x as html>` 目前解析报错；待 P1 #7/#8 语法扩展批次一并解决（已并入 P1 说明） |
| 2026-09-08 | P0-5 Commenter 动作测试需在断言前 `commitDocument` | 编辑器动作写入 Document 后 PSI 未提交时 `file.text` 是旧值，测试误报失败；动作本身行为正常 |
| 2026-09-08 | 新发现 P1 项：块级 `<#assign x>...</#assign>` 解析缺口 | `assign_directive` 规则强制要求 `= expr`，块赋值形式产生解析错误节点，真实模板常见，属「看代码」正确性问题，需 BNF 扩展 + 重新生成解析器 |
| 2026-09-08 | P1 #7/#8 表达式扩展采用「单层扁平循环」（`primary (DOT primary \| ?id \| ?? \| cmp primary)*`），不引入 comparison/postfix 中间规则 | 现有引用逻辑（`FtlExpression.primaryList`、`expr.firstChild`、`.withParent(FtlPrimary)`）依赖 PRIMARY 是 EXPRESSION 的直接子节点；中间规则会打断这些访问器 |
| 2026-09-08 | 词形式比较运算符（`gt`/`gte`/`lt`/`lte`）暂缓 | 它们是软关键字：直接做成 token 会破坏 `gt` 等作变量名/宏名的模板；后续如需支持应走 GrammarKit external 谓词或解析期文本检查 |
| 2026-09-08 | `??`（存在判断）随 #7 一并支持（而非留到 #12 运算符批次） | 真实模板中出现频率极高（`<#if x??>`），且实现成本仅一个 token + 一条循环分支 |

## 实施记录

- 2026-09-08：P0-1 ~ P0-6 全部完成，`gradlew test` 全量通过（74 用例，0 失败）。
  新增/更新测试：`FreemarkerLexerTest`（`#{` 插值、`\` 反斜杠语义）、`FtlReferenceTest`（宏参数解析 ×2、
  多级 import 路径 ×1）、`FtlStructureViewTest`（compress/noparse 折叠、注释折叠）、
  `FtlCommenterTest`（EP 注册 + Ctrl+/ 动作行为，新增测试类）。
- 2026-09-08：P1 #7 + #8 完成（含顺带项：generic 指令 `as`、块级 assign 解析+折叠、插值态比较运算、
  `??` 存在判断），BNF/flex 变更后 `generateParser`+`generateLexer` 重新生成，`gradlew test` 全量通过
  （97 用例，0 失败）。新增 `FtlExpressionParseTest`（16 用例）；`FtlReferenceTest` 补内建函数相关
  引用用例 ×3；`FreemarkerLexerTest` 补括号比较/插值比较 token 用例 ×3。版本号 0.3.2 → 0.4.0。
