package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

open class FtlTokenType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FtlTokenType.$debugName"
}
