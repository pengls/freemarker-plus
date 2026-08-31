package com.freemarkerplus.navigation

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement

/**
 * 为 FTL 元素提供 Find Usages 处理器。默认 FindUsagesHandler 通过
 * ReferencesSearch 收集引用，正好命中我们注册的 referencesSearch searcher。
 */
class FtlFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean = element.language.id == "FreemarkerPlus"

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler =
        object : FindUsagesHandler(element) {}
}
