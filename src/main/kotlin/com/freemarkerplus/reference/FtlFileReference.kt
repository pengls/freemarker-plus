package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

class FtlFileReference(element: FtlStringLiteral, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement) {

    private val delegate: FileReferenceSet by lazy {
        // The 2026.2 FileReferenceSet constructor takes the raw path string (quotes
        // stripped) plus the offset of that path inside the element.
        val path = myElement.text.substring(rangeInElement.startOffset, rangeInElement.endOffset)
        FileReferenceSet(path, myElement, rangeInElement.startOffset, null, true, true, null)
    }

    // FileReferenceSet splits the path on '/', producing one FileReference per
    // segment; the last one targets the file itself (the earlier ones target
    // intermediate directories).
    override fun resolve(): PsiElement? = delegate.allReferences.lastOrNull()?.resolve()

    override fun getVariants(): Array<Any> = delegate.allReferences.lastOrNull()?.variants ?: emptyArray()
}
