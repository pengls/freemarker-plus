package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerTokenTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.tree.IElementType

object FreemarkerColors {
    @JvmField val COMMENT = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    @JvmField val STRING = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_STRING", DefaultLanguageHighlighterColors.STRING)
    @JvmField val KEYWORD = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val DIRECTIVE_NAME = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_DIRECTIVE_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
    @JvmField val INTERPOLATION = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_INTERPOLATION", DefaultLanguageHighlighterColors.BRACES)
    @JvmField val NUMBER = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    @JvmField val OPERATOR = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    @JvmField val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
        "FREEMARKER_PLUS_BAD_CHARACTER", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)

    fun keyFor(tokenType: IElementType): TextAttributesKey? = when (tokenType) {
        FreemarkerTokenTypes.COMMENT -> COMMENT
        FreemarkerTokenTypes.STRING -> STRING
        FreemarkerTokenTypes.KEYWORD -> KEYWORD
        FreemarkerTokenTypes.DIRECTIVE_NAME -> DIRECTIVE_NAME
        FreemarkerTokenTypes.INTERPOLATION -> INTERPOLATION
        FreemarkerTokenTypes.NUMBER -> NUMBER
        FreemarkerTokenTypes.OPERATOR -> OPERATOR
        FreemarkerTokenTypes.BAD_CHARACTER -> BAD_CHARACTER
        else -> null
    }
}
