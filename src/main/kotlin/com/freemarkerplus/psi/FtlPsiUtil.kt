package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
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

    // 收集指定名字的变量声明（assign/local/global 赋值名、list 的 as 循环变量、
    // macro/function 的参数名——宏体内的 ${param} 由此可跳转/查找引用/重命名）。
    fun findVariableDeclarations(file: FtlFile, name: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        fun add(ident: PsiElement?) {
            if (ident is FtlIdentifier && ident.text == name) result.add(ident)
        }
        PsiTreeUtil.processElements(file) { el ->
            when (el) {
                is FtlAssignDirective -> add(el.identifier)  // assign/local/global 赋值名
                is FtlListDirective -> add(el.identifier)    // list 的 as 循环变量
                is FtlMacroDirective -> el.attributeList.forEach { add(it.declaredParamName()) }
                is FtlFunctionDirective -> el.attributeList.forEach { add(it.declaredParamName()) }
                else -> {}
            }
            true
        }
        return result
    }

    // 参数声明名：具名默认值（a=1）取 attribute 的直接 identifier；
    // 位置参数（a）是 expression（primary→identifier），从表达式子树取首个标识符。
    private fun FtlAttribute.declaredParamName(): FtlIdentifier? =
        identifier ?: expression.let { PsiTreeUtil.findChildOfType(it, FtlIdentifier::class.java) }

    // 生成一个携带指定文本的独立 FtlIdentifier：解析一个哑 <#macro name> 并取其中的
    // name 标识符。重命名（setName / FtlIdentifierManipulator）用它作为替换元素，
    // 以保证替换节点带有正确的 PSI 结构/缩进信息（直接 new LeafPsiElement 会触发
    // PostprocessReformattingAspect 的缩进断言）。
    fun createIdentifier(project: Project, name: String): FtlIdentifier? {
        val dummy = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ftl", FreemarkerLanguage.INSTANCE, "<#macro $name>")
            ?: return null
        return PsiTreeUtil.findChildOfType(dummy, FtlMacroDirective::class.java)?.identifier
    }
}
