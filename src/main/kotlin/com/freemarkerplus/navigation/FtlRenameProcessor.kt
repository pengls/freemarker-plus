package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlIdentifier
import com.freemarkerplus.psi.FtlImportDirective
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * FTL 重命名处理器（Shift+F6）。
 *
 * 声明名（<#macro>/<#function>/<#assign>/<#list> 的 name 标识符）现在通过
 * FtlIdentifierMixin 实现了 PsiNameIdentifierOwner，平台因此会调用其 setName 改写声明名；
 * 引用处由默认的 findReferences（ReferencesSearch → FtlMethodUsageSearcher）统一收集并改名。
 * 这里只需把 FTL 元素声明为可处理，其余走默认实现。
 *
 * <#import "x.ftl" as ns> 的别名 ns 不在此列：它没有指向自己的引用（命名空间引用
 * lib.member 解析到被导入文件的成员声明，而非别名本身），改写它只会静默脱钩所有
 * ns.member 使用处。因此 canProcessElement 对 import 别名返回 false。
 *
 * 注意：caret 落在别名上时平台传入的 element 可能是 FtlIdentifier 本身，也可能是其下
 * 唯一的 IDENT 叶 token（FtlIdentifier 是包裹 IDENT 的复合节点），两种情况都要识别。
 * 实际阻断改名由 FtlIdentifierMixin.setName 兜底（返回 false 只会让平台回退到默认处理器）。
 */
class FtlRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        if (element.language.id != "FreemarkerPlus") return false
        val identifier = element as? FtlIdentifier ?: element.parent as? FtlIdentifier
        return identifier?.parent !is FtlImportDirective
    }
}
