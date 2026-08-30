package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlImportDirective
import com.freemarkerplus.psi.FtlIncludeDirective
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class FtlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlStringLiteral::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val literal = element as FtlStringLiteral
                    return if (isFileReferenceHost(literal)) {
                        arrayOf(FtlFileReference(literal, rangeOf(literal)))
                    } else {
                        emptyArray()
                    }
                }
            }
        )
    }

    private fun isFileReferenceHost(element: FtlStringLiteral): Boolean {
        // parent is the string_literal inside an include/import directive.
        val parent = element.parent
        return parent is FtlIncludeDirective || parent is FtlImportDirective
    }

    private fun rangeOf(element: FtlStringLiteral): TextRange =
        TextRange(1, element.textLength - 1)
}
