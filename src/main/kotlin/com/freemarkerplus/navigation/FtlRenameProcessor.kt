package com.freemarkerplus.navigation

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * FTL 重命名处理器（Shift+F6）。
 *
 * 声明名（<#macro>/<#function>/<#assign>/<#list>/<#import> 的 name 标识符）现在通过
 * FtlIdentifierMixin 实现了 PsiNameIdentifierOwner，平台因此会调用其 setName 改写声明名；
 * 引用处由默认的 findReferences（ReferencesSearch → FtlMethodUsageSearcher）统一收集并改名。
 * 这里只需把 FTL 元素声明为可处理，其余走默认实现。
 */
class FtlRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean =
        element.language.id == "FTL"
}
