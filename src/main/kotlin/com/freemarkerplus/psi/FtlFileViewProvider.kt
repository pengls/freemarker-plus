package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.PsiPlainTextFileImpl
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.tree.IElementType

/**
 * 模板语言双文件 view provider（官方 FTL 插件同款架构）：
 * - base language 文件：`FtlFile`（FTL 解析器，数据区为 `TEMPLATE_TEXT` 叶节点）
 * - template data language 文件：HTML/XML `PsiFile`，根内容元素为
 *   [FtlFileElementTypes.TEMPLATE_DATA] chameleon，展开后是真正的 HTML PSI。
 *
 * 平台据此让 .ftl 的 HTML/JS 区域获得原生导航（onclick 事件属性 → script 函数、
 * script src → 文件等）。
 */
class FtlFileViewProvider(
    manager: PsiManager,
    private val file: VirtualFile,
    physical: Boolean,
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, physical),
    TemplateLanguageFileViewProvider {

    override fun getBaseLanguage(): Language = FreemarkerLanguage.INSTANCE

    override fun getTemplateDataLanguage(): Language =
        if (file.extension == "ftlx") XMLLanguage.INSTANCE else HTMLLanguage.INSTANCE

    override fun getLanguages(): Set<Language> =
        setOf(baseLanguage, templateDataLanguage)

    override fun createFile(lang: Language): PsiFile? {
        return if (lang == baseLanguage) {
            LanguageParserDefinitions.INSTANCE.forLanguage(lang)!!.createFile(this)
        } else if (lang == templateDataLanguage) {
            val definition = LanguageParserDefinitions.INSTANCE.forLanguage(lang)
            val psiFile = definition?.createFile(this) ?: PsiPlainTextFileImpl(this)
            (psiFile as PsiFileImpl).setContentElementType(getContentElementType(lang)!!)
            psiFile
        } else {
            null
        }
    }

    override fun getContentElementType(language: Language): IElementType? =
        if (language == templateDataLanguage) FtlFileElementTypes.TEMPLATE_DATA else null

    override fun cloneInner(copy: VirtualFile): FtlFileViewProvider =
        FtlFileViewProvider(manager, copy, false)
}
