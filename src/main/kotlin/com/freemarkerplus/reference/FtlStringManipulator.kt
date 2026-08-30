package com.freemarkerplus.reference

import com.freemarkerplus.lang.FreemarkerLanguage
import com.freemarkerplus.psi.FtlIncludeDirective
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

class FtlStringManipulator : AbstractElementManipulator<FtlStringLiteral>() {

    override fun handleContentChange(element: FtlStringLiteral, range: TextRange, newContent: String): FtlStringLiteral? {
        val text = element.text
        val newText = text.substring(0, range.startOffset) + newContent + text.substring(range.endOffset)
        // The 2026.2 platform has no FTL-aware createExpressionFromText (PsiUtilCore's
        // getElementFactory is the JVM factory), so build a temporary FtlFile and take
        // its string literal as the replacement.
        val dummy = PsiFileFactory.getInstance(element.project)
            .createFileFromText("dummy.ftl", FreemarkerLanguage.INSTANCE, "<#include $newText>")
            ?: return null
        val include = PsiTreeUtil.findChildOfType(dummy, FtlIncludeDirective::class.java) ?: return null
        return element.replace(include.stringLiteral) as? FtlStringLiteral
    }

    override fun getRangeInElement(element: FtlStringLiteral): TextRange {
        // Strip the surrounding quotes so the reference targets the path text inside them.
        val text = element.text
        return if (text.length >= 2 && (text.first() == '"' || text.first() == '\'')) {
            TextRange(1, text.length - 1)
        } else {
            TextRange(0, text.length)
        }
    }
}
