package com.freemarkerplus.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object FtlPsiUtil {
    // 收集指定名字的宏/函数声明，返回名字标识符（跳转目标高亮名字）
    fun findMacroDeclarations(file: FtlFile, name: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        PsiTreeUtil.processElements(file) { el ->
            val ident = when (el) {
                is FtlMacroDirective -> el.identifier     // <#macro name>
                is FtlFunctionDirective -> el.identifier  // <#function name>
                else -> null
            }
            if (ident != null && ident.text == name) result.add(ident)
            true
        }
        return result
    }
}
