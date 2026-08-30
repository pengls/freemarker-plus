package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

open class FtlElementType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    override fun toString(): String = "FtlElementType.$debugName"
}
