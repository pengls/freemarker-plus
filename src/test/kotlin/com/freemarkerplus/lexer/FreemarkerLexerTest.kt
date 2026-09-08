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

    fun testBackslashBeforeInterpolationIsPlainText() {
        // FreeMarker 语义：模板文本中 `\` 不是转义符，\${x} = 文本 `\` + 插值 ${x}。
        // 高亮与解析两侧行为一致：反斜杠归数据区，${x} 照常成插值。
        val text = "\\\${x}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.TEMPLATE_DATA,
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testLegacyInterpolation() {
        val text = "#{x}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,
                FreemarkerTokenTypes.IDENTIFIER,
                FreemarkerTokenTypes.INTERPOLATION
            ),
            types(text)
        )
    }

    fun testParenComparisonInTag() {
        // 括号内的 > 是比较运算符，不结束指令；括号外的 > 结束指令
        val text = "<#if (a > b)>"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,   // <#
                FreemarkerTokenTypes.DIRECTIVE_NAME,  // if
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.OPERATOR,        // (
                FreemarkerTokenTypes.IDENTIFIER,      // a
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.OPERATOR,        // >
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.IDENTIFIER,      // b
                FreemarkerTokenTypes.OPERATOR,        // )
                FreemarkerTokenTypes.INTERPOLATION    // >
            ),
            types(text)
        )
    }

    fun testInterpolationComparison() {
        val text = "\${a > b}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,   // ${
                FreemarkerTokenTypes.IDENTIFIER,      // a
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.OPERATOR,        // >
                TokenType.WHITE_SPACE,
                FreemarkerTokenTypes.IDENTIFIER,      // b
                FreemarkerTokenTypes.INTERPOLATION    // }
            ),
            types(text)
        )
    }

    fun testBuiltinInInterpolationTokens() {
        // ? 内建：高亮层一直按 OPERATOR 处理，回归确认仍正常
        val text = "\${x?size}"
        assertEquals(
            listOf(
                FreemarkerTokenTypes.INTERPOLATION,   // ${
                FreemarkerTokenTypes.IDENTIFIER,      // x
                FreemarkerTokenTypes.OPERATOR,        // ?
                FreemarkerTokenTypes.IDENTIFIER,      // size
                FreemarkerTokenTypes.INTERPOLATION    // }
            ),
            types(text)
        )
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
