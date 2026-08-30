package com.freemarkerplus.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FreemarkerLexerTest : BasePlatformTestCase() {

    private fun lex(text: String): List<Triple<IElementType?, Int, Int>> {
        val lexer = FreemarkerLexer()
        lexer.start(text, 0, text.length)
        val result = mutableListOf<Triple<IElementType?, Int, Int>>()
        while (lexer.tokenType != null) {
            result.add(Triple(lexer.tokenType, lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    private fun types(text: String): List<IElementType?> = lex(text).map { it.first }

    fun testInterpolation() {
        val text = "\${user.name}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.OPERATOR,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testString() {
        val text = "\${'hello'}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.STRING,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testDirective() {
        val text = "<#if x>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testClosingDirective() {
        val text = "</#if>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testComment() {
        val text = "<#-- hello -->"
        assertEquals(listOf(FreemarkerTokenTypes.COMMENT), types(text))
    }

    fun testEscapedInterpolationIsData() {
        val text = "\\\${x}"
        assertEquals(listOf(FreemarkerTokenTypes.TEMPLATE_DATA), types(text))
    }

    fun testPlainHtmlIsData() {
        val text = "<div class=\"a\">text</div>"
        assertEquals(listOf(FreemarkerTokenTypes.TEMPLATE_DATA), types(text))
    }

    fun testStyleBlockContentIsStyleData() {
        val text = "<style>.a { color: red; }</style>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,   // <style>
                FreemarkerTokenTypes.STYLE_DATA,          // .a { color: red; }
                FreemarkerTokenTypes.TEMPLATE_DATA    // </style>
            ),
            types(text)
        )
    }

    fun testScriptBlockContentIsScriptData() {
        val text = "<script>var x = 1;</script>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,   // <script>
                FreemarkerTokenTypes.SCRIPT_DATA,         // var x = 1;
                FreemarkerTokenTypes.TEMPLATE_DATA    // </script>
            ),
            types(text)
        )
    }

    fun testEmptyScriptTagHasNoZeroLengthToken() {
        val text = "<script src=\"x.js\"></script>"
        assertEquals(
            listOf(FreemarkerTokenTypes.TEMPLATE_DATA, FreemarkerTokenTypes.TEMPLATE_DATA),
            types(text)
        )
    }

    fun testEmptyStyleTagHasNoZeroLengthToken() {
        val text = "<style></style>"
        assertEquals(
            listOf(FreemarkerTokenTypes.TEMPLATE_DATA, FreemarkerTokenTypes.TEMPLATE_DATA),
            types(text)
        )
    }

    fun testClosingMacroRecognized() {
        val text = "</@base.layout>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                FreemarkerTokenTypes.OPERATOR,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testClosingDirectiveAfterDataRecognized() {
        val text = "text</#if>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.DIRECTIVE_NAME,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }
}
