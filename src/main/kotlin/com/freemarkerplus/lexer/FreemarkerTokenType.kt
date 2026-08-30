package com.freemarkerplus.lexer

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

class FreemarkerTokenType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FreemarkerTokenType.$debugName"
}

object FreemarkerTokenTypes {
    @JvmField val COMMENT = FreemarkerTokenType("COMMENT")
    @JvmField val STRING = FreemarkerTokenType("STRING")
    @JvmField val KEYWORD = FreemarkerTokenType("KEYWORD")
    @JvmField val DIRECTIVE_NAME = FreemarkerTokenType("DIRECTIVE_NAME")
    @JvmField val INTERPOLATION = FreemarkerTokenType("INTERPOLATION")
    @JvmField val IDENTIFIER = FreemarkerTokenType("IDENTIFIER")
    @JvmField val NUMBER = FreemarkerTokenType("NUMBER")
    @JvmField val OPERATOR = FreemarkerTokenType("OPERATOR")
    @JvmField val BAD_CHARACTER = FreemarkerTokenType("BAD_CHARACTER")
    @JvmField val TEMPLATE_DATA = FreemarkerTokenType("TEMPLATE_DATA")
    @JvmField val STYLE_DATA = FreemarkerTokenType("STYLE_DATA")
    @JvmField val SCRIPT_DATA = FreemarkerTokenType("SCRIPT_DATA")
}
