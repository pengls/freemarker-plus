package com.freemarkerplus.navigation

import com.freemarkerplus.lang.FreemarkerFileType
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlIdentifier
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor

/**
 * 找引用（referencesSearch）：给定一个声明标识符，跨文件返回所有引用它的标识符。
 *
 * 遍历项目内所有 FTL 文件，对每个文本等于声明名的非声明标识符，复用其真实
 * reference（宏调用/变量/命名空间引用）交给消费者。声明标识符本身不携带
 * reference，天然被排除，因此不会把声明当成使用处。
 */
class FtlMethodUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val declaration = queryParameters.elementToSearch
        if (declaration !is FtlIdentifier) return
        if (!declaration.isDeclaration()) return
        val name = declaration.text
        if (name.isEmpty()) return

        val project = declaration.project
        val scope = queryParameters.effectiveSearchScope
        val files = FileTypeIndex.getFiles(FreemarkerFileType, GlobalSearchScope.allScope(project))
        for (vf in files) {
            // 尊重 Find Usages 传入的搜索范围（如「在目录/文件中查找」），
            // 而不是始终返回项目级全部结果。
            if (!scope.contains(vf)) continue
            val file = PsiManager.getInstance(project).findFile(vf) as? FtlFile ?: continue
            for (ident in PsiTreeUtil.findChildrenOfType(file, FtlIdentifier::class.java)) {
                if (ident.text != name) continue
                if (ident.isDeclaration()) continue
                for (ref in ident.references) {
                    if (!consumer.process(ref)) return
                }
            }
        }
    }
}
