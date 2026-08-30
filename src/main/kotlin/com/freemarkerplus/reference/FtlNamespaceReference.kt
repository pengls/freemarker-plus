package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlExpression
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlImportDirective
import com.freemarkerplus.psi.FtlPrimary
import com.freemarkerplus.psi.FtlPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil

/**
 * 命名空间引用：`<#import "lib.ftl" as lib>` 后 `lib.hello` 的根段 `lib`，
 * 解析到 lib.ftl 里名为 `hello` 的宏/函数/变量声明。
 *
 * element 是限定名表达式的根标识符（`lib`），成员名取自表达式的第二个 primary
 * （`hello`）；与变量引用（FtlVariableReference）共存于同一根标识符上。
 */
class FtlNamespaceReference(element: PsiElement, rangeInElement: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val namespace = element.text
        val member = memberName() ?: return emptyArray()
        val file = element.containingFile as? FtlFile ?: return emptyArray()
        val importedFile = findImportedFile(file, namespace) ?: return emptyArray()
        val declarations =
            FtlPsiUtil.findMacroDeclarations(importedFile, member) +
                FtlPsiUtil.findVariableDeclarations(importedFile, member)
        return declarations.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    // 成员名来自表达式第二个 primary（lib.hello → hello）；根段 primary 之后才有成员。
    private fun memberName(): String? {
        val primary = element.parent as? FtlPrimary ?: return null
        val expr = primary.parent as? FtlExpression ?: return null
        val primaries = expr.primaryList
        if (primaries.size < 2) return null
        return primaries[1].identifier?.text
    }

    private fun findImportedFile(file: FtlFile, namespace: String): FtlFile? {
        val import = findImportDirective(file, namespace) ?: return null
        val path = import.stringLiteral.text.trim('\'', '"')
        val vf = file.virtualFile?.parent?.findChild(path) ?: return null
        return PsiManager.getInstance(file.project).findFile(vf) as? FtlFile
    }

    override fun getVariants(): Array<Any> = emptyArray()

    companion object {
        // import 指令被外层 outer_element 包裹，须递归查找（getChildrenOfType 只查直接子节点）。
        // 供 FtlReferenceContributor 复用：判断根标识符是否为已导入的命名空间别名。
        fun findImportDirective(file: FtlFile, namespace: String): FtlImportDirective? =
            PsiTreeUtil.findChildrenOfType(file, FtlImportDirective::class.java)
                .firstOrNull { it.identifier.text == namespace }
    }
}
