package com.freemarkerplus.psi

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.freemarkerplus.lang.FreemarkerLanguage

class FtlParserTest : BasePlatformTestCase() {

    private fun parse(text: String): FtlFile {
        val factory = PsiFileFactory.getInstance(project)
        return factory.createFileFromText("test.ftl", FreemarkerLanguage.INSTANCE, text) as FtlFile
    }

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
}
