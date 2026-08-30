package com.freemarkerplus.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object FreemarkerFileType : LanguageFileType(FreemarkerLanguage.INSTANCE) {
    override fun getName(): String = "FreeMarker Template"
    override fun getDescription(): String = "FreeMarker template file"
    override fun getDefaultExtension(): String = "ftl"
    override fun getIcon(): Icon = AllIcons.FileTypes.Html
}
