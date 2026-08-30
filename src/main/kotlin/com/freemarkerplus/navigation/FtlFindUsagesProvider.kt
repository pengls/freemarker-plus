package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlMacroDirective
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

/**
 * 提供给 Find Usages 视图/弹窗的展示信息：元素类型、描述名、节点文本。
 *
 * element 通常是声明名（宏/函数/变量的 name 标识符），故按父指令类型区分。
 */
class FtlFindUsagesProvider : FindUsagesProvider {
    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element.language.id == "FTL"

    override fun getHelpId(element: PsiElement): String = "reference.dialogs.findUsages"

    override fun getType(element: PsiElement): String = when (element.parent) {
        is FtlMacroDirective -> "Macro"
        is FtlFunctionDirective -> "Function"
        else -> "Variable"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        element.text.lines().firstOrNull()?.take(60) ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
