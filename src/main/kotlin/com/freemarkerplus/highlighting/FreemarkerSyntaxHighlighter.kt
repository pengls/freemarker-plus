package com.freemarkerplus.highlighting

import com.freemarkerplus.lexer.FreemarkerLexer
import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.freemarkerplus.lexer.FreemarkerTokenTypes

class FreemarkerSyntaxHighlighter(
    private val project: Project?,
    private val virtualFile: VirtualFile?
) : SyntaxHighlighterBase() {

    private val htmlHighlighter: SyntaxHighlighter? by lazy {
        SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, project, virtualFile)
    }
    private val cssHighlighter: SyntaxHighlighter? by lazy {
        val lang = Language.findLanguageByID("CSS")
        if (lang != null) SyntaxHighlighterFactory.getSyntaxHighlighter(lang, project, virtualFile) else null
    }
    private val jsHighlighter: SyntaxHighlighter? by lazy {
        val lang = Language.findLanguageByID("JavaScript")
        if (lang != null) SyntaxHighlighterFactory.getSyntaxHighlighter(lang, project, virtualFile) else null
    }

    override fun getHighlightingLexer(): Lexer {
        val layered = LayeredLexer(FreemarkerLexer())
        htmlHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.TEMPLATE_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        cssHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.STYLE_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        jsHighlighter?.let {
            layered.registerSelfStoppingLayer(
                it.highlightingLexer,
                arrayOf(FreemarkerTokenTypes.SCRIPT_DATA),
                IElementType.EMPTY_ARRAY
            )
        }
        return layered
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        FreemarkerColors.keyFor(tokenType)?.let { return pack(it) }
        htmlHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        cssHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        jsHighlighter?.getTokenHighlights(tokenType)?.let { return it }
        return TextAttributesKey.EMPTY_ARRAY
    }
}
