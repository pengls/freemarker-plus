package com.freemarkerplus.navigation

import com.freemarkerplus.lang.FreemarkerLanguage
import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlFileViewProvider
import com.freemarkerplus.psi.FtlInterpolation
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Phase 2.5：数据区模板语言机制（双文件 view provider）验证。
 *
 * .ftl 的 HTML/CSS/JS 数据区由 FtlFileViewProvider 的「template data language 文件」
 * （HTML/XML PsiFile）承载，根内容元素为 TEMPLATE_DATA chameleon，展开后是真实 HTML PSI。
 * 本测试验证：数据文件存在、HTML PSI 结构（标签/属性）、FTL 构造与 HTML PSI 共存。
 *
 * JS 场景（script 内容 → JS PSI、onclick → script 函数、外部 .js、script src 文件引用）
 * 依赖平台 JavaScript 插件，单元测试环境无法加载（见 Phase 2.5 spike 结论），
 * 由 runIde 人工验收覆盖。
 */
class FtlHtmlNavigationTest : BasePlatformTestCase() {

    private fun fileText(): String = myFixture.file.text

    private fun dataFile(): PsiElement? {
        val vp = myFixture.file.viewProvider
        val dataLang = vp.languages.firstOrNull { it.id != "FTL" } ?: return null
        return vp.getPsi(dataLang)
    }

    // ---------- 双文件 view provider ----------

    fun testViewProviderIsTwoFileTemplateLanguageProvider() {
        myFixture.configureByText("main.ftl", "<div>hello</div>")
        val vp = myFixture.file.viewProvider
        assertTrue("expected FtlFileViewProvider", vp is FtlFileViewProvider)
        assertEquals(setOf(FreemarkerLanguage.INSTANCE, HTMLLanguage.INSTANCE), vp.languages)
        assertTrue("main file should be FtlFile", myFixture.file is FtlFile)
        val data = dataFile()
        assertNotNull("expected an HTML data file", data)
        assertEquals("HTML", data!!.language.id)
    }

    fun testFtlxUsesXmlDataLanguage() {
        myFixture.configureByText("main.ftlx", "<root>hello</root>")
        val data = dataFile()
        assertNotNull("expected an XML data file for .ftlx", data)
        assertEquals(XMLLanguage.INSTANCE, data!!.language)
    }

    // ---------- HTML PSI 结构 ----------

    fun testDataRegionExpandsToXmlTag() {
        myFixture.configureByText("main.ftl", "<div class=\"a\">hello</div>")
        val tags = PsiTreeUtil.findChildrenOfType(dataFile(), XmlTag::class.java)
        assertTrue("expected XmlTag in data file, text=${fileText()}", tags.isNotEmpty())
        assertEquals("div", tags.first().name)
        assertEquals("a", tags.first().getAttributeValue("class"))
    }

    fun testDataRegionExpandsToAttribute() {
        myFixture.configureByText("main.ftl", "<button onclick=\"login()\">x</button>")
        val attrs = PsiTreeUtil.findChildrenOfType(dataFile(), XmlAttribute::class.java)
        assertTrue("expected XmlAttribute children in data file", attrs.isNotEmpty())
        assertTrue(attrs.any { it.name == "onclick" })
        assertEquals("login()", attrs.first { it.name == "onclick" }.value)
    }

    fun testHtmlDataFileTextPreserved() {
        myFixture.configureByText("main.ftl", "<div>hello</div>")
        assertEquals(fileText(), dataFile()!!.text)
    }

    // ---------- FTL 与 HTML 共存 ----------

    fun testFtlDirectiveStillParsedAlongsideHtml() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n<div>\${user}</div>")
        assertNotNull(PsiTreeUtil.findChildOfType(myFixture.file, FtlAssignDirective::class.java))
        assertNotNull(PsiTreeUtil.findChildOfType(myFixture.file, FtlInterpolation::class.java))
        val tags = PsiTreeUtil.findChildrenOfType(dataFile(), XmlTag::class.java)
        assertTrue("expected XmlTag for the div in data file", tags.isNotEmpty())
        // 插值在数据文件里是 FTL 外语言元素，div 文本应保持完整
        assertTrue(tags.first().text.contains("\${user}"))
    }

    fun testInterpolationBetweenHtmlRegions() {
        myFixture.configureByText("main.ftl", "<span>a</span>\${name}<span>b</span>")
        assertNotNull(PsiTreeUtil.findChildOfType(myFixture.file, FtlInterpolation::class.java))
        val tags = PsiTreeUtil.findChildrenOfType(dataFile(), XmlTag::class.java)
        assertEquals(2, tags.size)
    }

    // ---------- 平台 HTML 导航能力（不依赖 JS 插件） ----------

    fun testHtmlTagNameNavigationElement() {
        myFixture.configureByText("main.ftl", "<form action=\"/login\"></form>")
        val tags = PsiTreeUtil.findChildrenOfType(dataFile(), XmlTag::class.java)
        assertEquals(1, tags.size)
        assertEquals("form", tags.first().name)
        assertEquals("/login", tags.first().getAttributeValue("action"))
    }
}
