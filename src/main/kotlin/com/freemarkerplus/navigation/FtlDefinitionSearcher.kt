package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlIdentifier
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroDirective
import com.freemarkerplus.psi.FtlPsiUtil
import com.freemarkerplus.reference.FtlFileIndex
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex

/**
 * 跨文件找定义（definitionsSearch）：给定一个引用处标识符，返回所有同名声明。
 *
 * 平台在 Ctrl+B/找定义无直接 reference 解析可跳转时调用本 searcher 作为兜底；
 * sourceElement 是引用处元素（FtlIdentifier），其文本即声明名。声明本身不
 * 作为「找定义」的源。
 */
class FtlDefinitionSearcher : QueryExecutorBase<PsiElement, PsiElement>(true) {
    override fun processQuery(sourceElement: PsiElement, consumer: Processor<in PsiElement>) {
        if (sourceElement !is FtlIdentifier) return
        if (sourceElement.isDeclaration()) return
        val name = sourceElement.text
        if (name.isEmpty()) return

        val project = sourceElement.project
        val files = FileBasedIndex.getInstance()
            .getContainingFiles(FtlFileIndex.NAME, name, GlobalSearchScope.allScope(project))
        for (vf in files) {
            val file = PsiManager.getInstance(project).findFile(vf) as? FtlFile ?: continue
            for (decl in FtlPsiUtil.findMacroDeclarations(file, name)) {
                if (!consumer.process(decl)) return
            }
            for (decl in FtlPsiUtil.findVariableDeclarations(file, name)) {
                if (!consumer.process(decl)) return
            }
        }
    }
}

/** 标识符是否为声明名（<#macro>/<#function>/<#assign>/<#list> 的 name）。 */
internal fun FtlIdentifier.isDeclaration(): Boolean =
    parent is FtlMacroDirective ||
        parent is FtlFunctionDirective ||
        parent is FtlAssignDirective ||
        parent is FtlListDirective
