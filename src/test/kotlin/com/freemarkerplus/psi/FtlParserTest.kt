package com.freemarkerplus.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.freemarkerplus.lang.FreemarkerLanguage

class FtlParserTest : BasePlatformTestCase() {

    private fun parse(text: String): FtlFile {
        val factory = PsiFileFactory.getInstance(project)
        return factory.createFileFromText("test.ftl", FreemarkerLanguage.INSTANCE, text) as FtlFile
    }

    private fun leaves(file: FtlFile): List<PsiElement> =
        PsiTreeUtil.collectElementsOfType(file, PsiElement::class.java).toList()

    fun testAssignDirectiveParsed() {
        val file = parse("<#assign user = \"a\">")
        val assign = PsiTreeUtil.findChildOfType(file, FtlAssignDirective::class.java)
        assertNotNull(assign)
        assertEquals("user", assign?.identifier?.text)
    }

    fun testInterpolationParsed() {
        val file = parse("hello \${user.name}")
        val interp = PsiTreeUtil.findChildOfType(file, FtlInterpolation::class.java)
        assertNotNull(interp)
        assertTrue(interp!!.text.contains("user.name"))
    }

    fun testLoneDollarIsTemplateData() {
        val file = parse("price \$5")
        assertFalse("expected no error elements", PsiTreeUtil.hasErrorElements(file))
        assertTrue(leaves(file).none { it.node?.elementType == TokenType.BAD_CHARACTER })
        assertTrue(leaves(file).any { it.node?.elementType == FtlElementTypes.TEMPLATE_DATA })
    }

    fun testHtmlClosingTagIsTemplateData() {
        val file = parse("<div>a</div>")
        assertFalse("expected no error elements", PsiTreeUtil.hasErrorElements(file))
        assertTrue(leaves(file).none { it.node?.elementType == TokenType.BAD_CHARACTER })
        val dataText = leaves(file)
            .filter { it.node?.elementType == FtlElementTypes.TEMPLATE_DATA }
            .joinToString("") { it.text }
        assertTrue(dataText.contains("</div>"))
    }

    fun testCommentLeafTokens() {
        val file = parse("<#-- a comment -->")
        val types = leaves(file).mapNotNull { it.node?.elementType }
        assertTrue(types.contains(FtlElementTypes.COMMENT_START))
        assertTrue(types.contains(FtlElementTypes.COMMENT_END))
    }

    fun testHtmlDataIsTemplateDataLeaf() {
        val file = parse("<div>hello</div>")
        val dataLeaves = leaves(file).filter { it.node?.elementType == FtlElementTypes.TEMPLATE_DATA }
        assertTrue(dataLeaves.isNotEmpty())
    }

    fun testMixedContentParsed() {
        val file = parse("<div>\${user}</div><#if x>y</#if>")
        assertNotNull(PsiTreeUtil.findChildOfType(file, FtlInterpolation::class.java))
        assertNotNull(PsiTreeUtil.findChildOfType(file, FtlGenericDirective::class.java))
    }
}
