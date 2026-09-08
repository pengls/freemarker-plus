package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * P1 表达式扩展的解析测试：`?内建函数`（含参数）、`??`、括号内比较运算、
 * 插值内比较运算、块级 assign、generic 指令的 `as`。
 */
class FtlExpressionParseTest : BasePlatformTestCase() {

    private fun parse(text: String): FtlFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText("test.ftl", FreemarkerLanguage.INSTANCE, text) as FtlFile

    private fun errorNodeTypes(file: FtlFile): List<String> {
        val result = mutableListOf<String>()
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                val node = element.node
                if (node != null &&
                    (node.elementType == TokenType.ERROR_ELEMENT || node.elementType == TokenType.BAD_CHARACTER)
                ) {
                    result.add(node.elementType.toString() + ":" + node.text)
                }
                super.visitElement(element)
            }
        })
        return result
    }

    private fun assertNoErrors(text: String) {
        assertEquals("unexpected error nodes in: $text", emptyList<String>(), errorNodeTypes(parse(text)))
    }

    fun testBuiltinChainParsed() = assertNoErrors("\${list?size}")

    fun testBuiltinWithArgsParsed() = assertNoErrors("\${x?string(\"a\",\"b\")}")

    fun testBuiltinChainedTwice() = assertNoErrors("\${x?upper_case?length}")

    fun testBuiltinOnParenComparison() = assertNoErrors("\${(a > b)?string(\"y\",\"n\")}")

    fun testBuiltinOnDottedChain() = assertNoErrors("\${user.name?has_content}")

    fun testExistsOperatorInInterpolation() = assertNoErrors("\${x??}")

    fun testExistsOperatorInIf() = assertNoErrors("<#if user??>y</#if>")

    fun testParenComparisonInIf() = assertNoErrors("<#if (a > b)>x</#if>")

    fun testGeLeInIf() {
        assertNoErrors("<#if (a >= b)>x</#if>")
        assertNoErrors("<#if (a <= b)>x</#if>")
        assertNoErrors("<#if (a < b)>x</#if>")
    }

    fun testComparisonInInterpolation() {
        assertNoErrors("\${a > b}")
        assertNoErrors("\${a >= b}")
        assertNoErrors("\${a < b}")
    }

    fun testNestedParensComparison() = assertNoErrors("<#if ((a > b))>x</#if>")

    fun testAssignBlockFormParsed() = assertNoErrors("<#assign x>abc</#assign>")

    fun testAssignLocalBlockFormParsed() = assertNoErrors("<#macro m><#local y>1</#local></#macro>")

    fun testAssignPlainFormStillParsed() = assertNoErrors("<#assign x = 1>")

    fun testEscapeWithAsParsed() = assertNoErrors("<#escape x as html>e</#escape>")

    fun testDottedChainStillParsed() = assertNoErrors("\${user.name}")

    fun testBuiltinRootPrimaryStructure() {
        // 引用逻辑依赖：PRIMARY 仍是 EXPRESSION 的直接子节点（primaryList 不回退）
        val file = parse("\${list?size}")
        val interpolation = PsiTreeUtil.findChildOfType(file, FtlInterpolation::class.java)
        assertNotNull(interpolation)
        val primaries = interpolation!!.expression.primaryList
        assertEquals(1, primaries.size)
        assertEquals("list", primaries.single().identifier?.text)
    }
}
