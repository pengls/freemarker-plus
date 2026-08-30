package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerFileType
import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class FtlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FreemarkerLanguage.INSTANCE) {
    override fun getFileType(): FileType = FreemarkerFileType
}
