package com.freemarkerplus.lang

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

class FreemarkerLanguage private constructor() : Language("FTL"), TemplateLanguage {
    companion object {
        @JvmField
        val INSTANCE = FreemarkerLanguage()
    }
}
