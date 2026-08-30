package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlIdentifier
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

/**
 * ElementManipulator for {@code identifier}.
 *
 * The macro/variable/namespace references ({@code FtlMacroReference},
 * {@code FtlVariableReference}, {@code FtlNamespaceReference}) extend
 * {@code PsiPolyVariantReferenceBase}, whose {@code handleElementRename} rewrites the
 * reference's text range through this manipulator. FtlIdentifier is a single
 * {@code IDENT} token, so the whole text is the name; the replacement is built via
 * {@code FtlPsiUtil.createIdentifier} and swapped in with {@code PsiElement.replace}
 * (the same pattern as {@code FtlStringManipulator}).
 */
class FtlIdentifierManipulator : AbstractElementManipulator<FtlIdentifier>() {

    override fun handleContentChange(element: FtlIdentifier, range: TextRange, newContent: String): FtlIdentifier {
        val text = element.text
        val newText = text.substring(0, range.startOffset) + newContent + text.substring(range.endOffset)
        val replacement = FtlPsiUtil.createIdentifier(element.project, newText)
            ?: return element
        return element.replace(replacement) as FtlIdentifier
    }

    override fun getRangeInElement(element: FtlIdentifier): TextRange =
        TextRange(0, element.textLength)
}
