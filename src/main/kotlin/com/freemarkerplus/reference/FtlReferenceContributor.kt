package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlExpression
import com.freemarkerplus.psi.FtlIdentifier
import com.freemarkerplus.psi.FtlImportDirective
import com.freemarkerplus.psi.FtlIncludeDirective
import com.freemarkerplus.psi.FtlMacroCall
import com.freemarkerplus.psi.FtlPrimary
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class FtlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlStringLiteral::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val literal = element as FtlStringLiteral
                    return if (isFileReferenceHost(literal)) {
                        arrayOf(FtlFileReference(literal, rangeOf(literal)))
                    } else {
                        emptyArray()
                    }
                }
            }
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlIdentifier::class.java)
                .withParent(FtlMacroCall::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    // element 是 <@name 里的 name；</@name 的闭合名也指向同一宏，但只给打开标签附引用
                    return if (element.parent.text.startsWith("<@")) {
                        arrayOf(FtlMacroReference(element, TextRange(0, element.textLength)))
                    } else {
                        emptyArray()
                    }
                }
            }
        )

        // 表达式内的首段标识符（${user}、<#if user> 的 user 等）→ 同文件变量声明。
        // 只匹配 parent 为 FtlPrimary 的标识符，天然排除宏调用名（parent 为 FtlMacroCall）
        // 与声明名（parent 为 assign/list/macro 指令）；根段过滤在 provider 内完成。
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlIdentifier::class.java)
                .withParent(FtlPrimary::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    // 只对限定名根段挂引用（${user.name} → user）；属性段 name 属 Phase 3 Java 数据模型
                    val expr = PsiTreeUtil.getParentOfType(element, FtlExpression::class.java)
                        ?: return emptyArray()
                    val rootPrimary = expr.firstChild
                    return if (rootPrimary === element.parent) {
                        arrayOf(FtlVariableReference(element, TextRange(0, element.textLength)))
                    } else {
                        emptyArray()
                    }
                }
            }
        )

        // 命名空间引用：限定名根段（lib.hello → lib）若通过 <#import "lib.ftl" as lib> 引入，
        // 则跳转到被导入文件里的成员声明。与上面的变量引用共存于同一根标识符（multiResolve）。
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(FtlIdentifier::class.java)
                .withParent(FtlPrimary::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val expr = PsiTreeUtil.getParentOfType(element, FtlExpression::class.java)
                        ?: return emptyArray()
                    // 必须是根段，且是带 DOT 的限定名（至少两个 primary）。
                    if (expr.firstChild !== element.parent) return emptyArray()
                    if (expr.primaryList.size < 2) return emptyArray()
                    return arrayOf(FtlNamespaceReference(element, TextRange(0, element.textLength)))
                }
            }
        )
    }

    private fun isFileReferenceHost(element: FtlStringLiteral): Boolean {
        // parent is the string_literal inside an include/import directive.
        val parent = element.parent
        return parent is FtlIncludeDirective || parent is FtlImportDirective
    }

    private fun rangeOf(element: FtlStringLiteral): TextRange =
        TextRange(1, element.textLength - 1)
}
