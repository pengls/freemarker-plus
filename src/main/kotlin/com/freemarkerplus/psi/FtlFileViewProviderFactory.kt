package com.freemarkerplus.psi

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager

/**
 * Flat FTL PSI + 模板语言数据区：单根 view provider（[FtlFileViewProvider]）。
 *
 * `FreemarkerLanguage` 是 [com.intellij.psi.templateLanguages.TemplateLanguage]（Phase 1），
 * 平台默认会回退到 `TemplateFileViewProviderFactory`（无模板数据语言映射时返回 null），
 * 因此这里显式固定为 [FtlFileViewProvider]：单根 FTL PSI + 实现
 * `TemplateLanguageFileViewProvider`，让数据区 `TEMPLATE_DATA` chameleon 可按模板语言机制
 * 展开为 HTML PSI。
 */
class FtlFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider = FtlFileViewProvider(manager, file, eventSystemEnabled)
}
