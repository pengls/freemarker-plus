package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.tree.IElementType

open class FtlElementType(debugName: String) : IElementType(debugName, FreemarkerLanguage.INSTANCE) {
    // 不调用 IElementType.getDebugName()（@ApiStatus.Internal）——名字存进自己的字段。
    private val myDebugName: String = debugName

    override fun toString(): String = "FtlElementType.$myDebugName"
}
