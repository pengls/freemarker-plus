package com.freemarkerplus.navigation

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

/**
 * 编辑器顶部面包屑：为光标所在指令/宏调用显示上下文（仅指令节点，
 * 不含闭合标签与 outer_element 包装节点）。
 */
class FtlBreadcrumbsInfoProvider : BreadcrumbsProvider {

    override fun isShownByDefault(): Boolean = true

    override fun getLanguages(): Array<Language> = arrayOf(FreemarkerLanguage.INSTANCE)

    override fun acceptElement(element: PsiElement): Boolean = element.isStructuralDirective()

    override fun getParent(element: PsiElement): PsiElement? = element.parent

    override fun getElementInfo(element: PsiElement): String = element.openingTagText()

    override fun getElementTooltip(element: PsiElement): String = element.text
}
