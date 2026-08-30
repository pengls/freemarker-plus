package com.freemarkerplus.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class FreemarkerColorSettingsPage : ColorSettingsPage {

    private val descriptors = arrayOf(
        AttributesDescriptor("Comment", FreemarkerColors.COMMENT),
        AttributesDescriptor("String", FreemarkerColors.STRING),
        AttributesDescriptor("Keyword", FreemarkerColors.KEYWORD),
        AttributesDescriptor("Directive name", FreemarkerColors.DIRECTIVE_NAME),
        AttributesDescriptor("Interpolation delimiters", FreemarkerColors.INTERPOLATION),
        AttributesDescriptor("Number", FreemarkerColors.NUMBER),
        AttributesDescriptor("Operator", FreemarkerColors.OPERATOR),
        AttributesDescriptor("Bad character", FreemarkerColors.BAD_CHARACTER)
    )

    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter = FreemarkerSyntaxHighlighter(null, null)

    override fun getDemoText(): String = DEMO_TEXT

    override fun getDisplayName(): String = "Freemarker"

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = descriptors

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    companion object {
        private val DEMO_TEXT = """
            |<#-- This is a comment -->
            |<#if user.age gt 18>
            |  <div class="card">
            |    <h1>Welcome, ${'$'}{user.name}!</h1>
            |    <style>
            |      .card { color: green; }
            |    </style>
            |    <script>
            |      var greeting = "hello";
            |    </script>
            |  </div>
            |</#if>
        """.trimMargin()
    }
}
