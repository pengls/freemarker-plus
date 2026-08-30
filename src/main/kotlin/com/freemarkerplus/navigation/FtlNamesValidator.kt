package com.freemarkerplus.navigation

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

/**
 * FTL 标识符名校验（重命名对话框）。
 *
 * FreeMarker 标识符形如 [a-zA-Z_][a-zA-Z0-9_]*（与语法中 IDENT 的 lexer 正则一致）。
 * 通过 lang.namesValidator 注册后，重命名对话框会对输入名做校验并拒绝非法名，
 * 而不是在 FtlIdentifierMixin.setName / FtlPsiUtil.createIdentifier 中静默截断
 * （my-macro → my）或 no-op。isKeyword 返回 false：list 等词在 FTL 上下文里是合法标识符，
 * 且重命名对话框只用 isKeyword 做告警提示。
 */
class FtlNamesValidator : NamesValidator {
    private val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")

    override fun isIdentifier(name: String, project: Project): Boolean =
        identifierPattern.matches(name)

    override fun isKeyword(name: String, project: Project): Boolean = false
}
