package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class FtlVariableReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.text
        val file = element.containingFile as? FtlFile ?: return emptyArray()
        return FtlPsiUtil.findVariableDeclarations(file, name)
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
