package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlGenericDirective
import com.freemarkerplus.psi.FtlMacroCall
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlStructureViewTest : BasePlatformTestCase() {

    fun testFoldingRegionsForIf() {
        myFixture.configureByText("main.ftl", "<#if x>\nhello\n</#if>")
        val builder = FtlFoldingBuilder()
        val regions = builder.buildFoldRegions(myFixture.file.node, myFixture.editor.document)
        assertEquals(1, regions.size)
        assertEquals("<#if x>\nhello\n</#if>", regions.single().range.substring(myFixture.file.text))
        assertEquals("<#if x>", builder.getPlaceholderText(regions.single().element))
    }

    fun testFoldingRegionsForListMacroFunction() {
        myFixture.configureByText(
            "main.ftl",
            "<#list xs as x>\na\n</#list>\n<#macro m>\nb\n</#macro>\n<#function f>\nc\n</#function>",
        )
        val builder = FtlFoldingBuilder()
        val regions = builder.buildFoldRegions(myFixture.file.node, myFixture.editor.document)
        assertEquals(3, regions.size)
    }

    fun testFoldingHandlesNesting() {
        myFixture.configureByText("main.ftl", "<#if a>\n<#if b>\nx\n</#if>\n</#if>")
        val builder = FtlFoldingBuilder()
        val regions = builder.buildFoldRegions(myFixture.file.node, myFixture.editor.document)
        assertEquals(2, regions.size)
        val sorted = regions.sortedBy { it.range.startOffset }
        assertTrue(sorted[0].range.startOffset < sorted[1].range.startOffset)
        assertTrue(sorted[0].range.endOffset > sorted[1].range.endOffset)
    }

    fun testFoldingSkipsUnclosedBlock() {
        myFixture.configureByText("main.ftl", "<#if x>\nhello")
        val builder = FtlFoldingBuilder()
        val regions = builder.buildFoldRegions(myFixture.file.node, myFixture.editor.document)
        assertEquals(0, regions.size)
    }

    fun testStructureViewCollectsDirectivesNotClosingTags() {
        myFixture.configureByText("main.ftl", "<#if x>\nhello\n</#if>\n<@greet/>")
        val model = FtlStructureViewModel(myFixture.file, myFixture.editor)
        val labels = model.root.children.mapNotNull { it.presentation.presentableText }
        assertTrue(labels.contains("<#if x>"))
        assertTrue(labels.contains("<@greet/>"))
        assertFalse(labels.contains("</#if>"))
    }

    fun testBreadcrumbsAcceptsDirectives() {
        myFixture.configureByText("main.ftl", "<#if x>\nhello\n</#if>\n<@greet/>")
        val provider = FtlBreadcrumbsInfoProvider()
        assertTrue(provider.languages.any { it.id == "FreemarkerPlus" })

        val generic = PsiTreeUtil.findChildrenOfType(myFixture.file, FtlGenericDirective::class.java)
        val open = generic.first { !it.isClosingDirective() }
        val close = generic.first { it.isClosingDirective() }
        assertTrue(provider.acceptElement(open))
        assertFalse(provider.acceptElement(close))
        assertEquals("<#if x>", provider.getElementInfo(open))

        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        assertTrue(provider.acceptElement(call!!))
    }
}
