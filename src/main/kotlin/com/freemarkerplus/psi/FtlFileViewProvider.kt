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
            // 数据文件根内容元素 = TEMPLATE_DATA chameleon（懒展开为 HTML PSI）。
            // 注意：不覆盖 TemplateLanguageFileViewProvider.getContentElementType
            // （@ApiStatus.Experimental，验证器会告警）——平台创建文件只走 createFile，
            // 这里直接设置即可（MultiplePsiFilesPerDocumentFileViewProvider.getPsi
            // 仅调用 createFile，不查询该接口方法）。
            (psiFile as PsiFileImpl).setContentElementType(FtlFileElementTypes.TEMPLATE_DATA)
            psiFile
        } else {
            null
        }
    }

    override fun cloneInner(copy: VirtualFile): FtlFileViewProvider =
        FtlFileViewProvider(manager, copy, false)
}
