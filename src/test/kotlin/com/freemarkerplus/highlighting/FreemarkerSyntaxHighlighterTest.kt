package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerTokenTypes
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FreemarkerSyntaxHighlighterTest : BasePlatformTestCase() {

    fun testCommentMapsToCommentKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.COMMENT)
                .contains(FreemarkerColors.COMMENT)
        )
    }

    fun testStringMapsToStringKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.STRING)
                .contains(FreemarkerColors.STRING)
        )
    }

    fun testDirectiveNameMapsToDirectiveKey() {
        val highlighter = FreemarkerSyntaxHighlighter(null, null)
        assertTrue(
            highlighter.getTokenHighlights(FreemarkerTokenTypes.DIRECTIVE_NAME)
                .contains(FreemarkerColors.DIRECTIVE_NAME)
        )
    }
}
