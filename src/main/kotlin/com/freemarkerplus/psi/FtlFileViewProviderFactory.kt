package com.freemarkerplus.psi

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider

/**
 * Flat FTL PSI: a single-root view provider (no HTML data-language overlay).
 *
 * `FreemarkerLanguage` is a [com.intellij.psi.templateLanguages.TemplateLanguage] (Phase 1),
 * so the platform would otherwise fall back to `TemplateFileViewProviderFactory`, which
 * returns `null` when no template data language is mapped. This factory pins FTL to the
 * standard single-root view provider so `PsiFileFactory.createFileFromText` (and normal
 * file opening) produces an `FtlFile` via `FtlParserDefinition`.
 */
class FtlFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider = SingleRootFileViewProvider(manager, file, eventSystemEnabled)
}
