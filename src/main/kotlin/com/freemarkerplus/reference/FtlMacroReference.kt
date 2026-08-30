package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex

class FtlMacroReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.text
        val file = element.containingFile as? FtlFile ?: return emptyArray()
        val sameFile = FtlPsiUtil.findMacroDeclarations(file, name)
        if (sameFile.isNotEmpty()) {
            return sameFile.map { PsiElementResolveResult(it) }.toTypedArray()
        }
        // 同文件没有同名宏/函数时，回退到文件级索引，命中其他文件定义的宏。
        return findCrossFileDeclarations(name, element.project)
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

    private fun findCrossFileDeclarations(name: String, project: Project): List<PsiElement> {
        val files = FileBasedIndex.getInstance()
            .getContainingFiles(FtlFileIndex.NAME, name, GlobalSearchScope.allScope(project))
        return files.flatMap { vf ->
            val target = PsiManager.getInstance(project).findFile(vf) as? FtlFile
                ?: return@flatMap emptyList()
            FtlPsiUtil.findMacroDeclarations(target, name)
        }
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
