package com.freemarkerplus.lang

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

class FreemarkerLanguage private constructor() : Language("FreemarkerPlus"), TemplateLanguage {
    companion object {
        @JvmField
        val INSTANCE = FreemarkerLanguage()
    }
}
