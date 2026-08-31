package com.freemarkerplus.psi

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Spike S2 验证（双文件模板语言架构）：数据文件（HTML 侧）展开出真实 HTML PSI。
 * JS 相关场景（S3/S4/S5）因测试环境无法加载 JavaScript 插件（intellij.platform.smRunner
 * 未解析导致 JS 插件被排除）无法单测，见 docs/superpowers/plans/
 * 2026-08-30-freemarker-plus-phase25-spike-notes.md；正式实施的 runIde 验收覆盖。
 */
class FtlSpikeHtmlTest : BasePlatformTestCase() {

    /** template data 侧文件（HTML/XML PsiFile）。 */
    private fun dataFile(): com.intellij.psi.PsiElement? {
        val vp = myFixture.file.viewProvider
        val dataLang = vp.languages.firstOrNull { it.id != "FTL" } ?: return null
        return vp.getPsi(dataLang)
    }

    fun testDataFileExpandsToXmlTag() {
        myFixture.configureByText("main.ftl", "<div class=\"a\">hello</div>")
        val data = dataFile()
        assertNotNull("expected an HTML data file", data)
        val tags = PsiTreeUtil.findChildrenOfType(data, XmlTag::class.java)
        assertTrue("expected XmlTag in data file", tags.isNotEmpty())
        assertEquals("div", tags.first().name)
    }

    fun testDataRegionExpandsToAttribute() {
        myFixture.configureByText("main.ftl", "<button onclick=\"login()\">x</button>")
        val attrs = PsiTreeUtil.findChildrenOfType(dataFile(), XmlAttribute::class.java)
        assertTrue("expected XmlAttribute children in data file", attrs.isNotEmpty())
        assertTrue(attrs.any { it.name == "onclick" })
    }

    fun testFtlDirectiveStillParsedAlongsideHtml() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n<div>\${user}</div>")
        assertNotNull(PsiTreeUtil.findChildOfType(myFixture.file, FtlAssignDirective::class.java))
        assertNotNull(PsiTreeUtil.findChildOfType(myFixture.file, FtlInterpolation::class.java))
        val tags = PsiTreeUtil.findChildrenOfType(dataFile(), XmlTag::class.java)
        assertTrue("expected XmlTag for the div in data file", tags.isNotEmpty())
    }
}
