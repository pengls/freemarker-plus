package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlMacroCall
import com.freemarkerplus.psi.FtlMacroDirective
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlReferenceTest : BasePlatformTestCase() {

    fun testIncludeResolvesToFile() {
        val inc = myFixture.addFileToProject("inc.ftl", "<#-- inc -->")
        myFixture.configureByText("main.ftl", "<#include \"inc.ftl\">")
        val literal = PsiTreeUtil.findChildOfType(myFixture.file, FtlStringLiteral::class.java)
        assertNotNull(literal)
        // caret inside the path text, between the quotes
        myFixture.editor.caretModel.moveToOffset(literal!!.textOffset + 2)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals(inc.virtualFile, ref?.resolve()?.containingFile?.virtualFile)
    }

    fun testImportResolvesToFile() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#-- lib -->")
        myFixture.configureByText("main.ftl", "<#import \"lib.ftl\" as lib>")
        val literal = PsiTreeUtil.findChildOfType(myFixture.file, FtlStringLiteral::class.java)
        assertNotNull(literal)
        myFixture.editor.caretModel.moveToOffset(literal!!.textOffset + 2)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals(lib.virtualFile, ref?.resolve()?.containingFile?.virtualFile)
    }

    fun testNestedIncludeResolvesToFile() {
        val inc = myFixture.addFileToProject("partials/inc.ftl", "<#-- inc -->")
        myFixture.configureByText("main.ftl", "<#include \"partials/inc.ftl\">")
        val literal = PsiTreeUtil.findChildOfType(myFixture.file, FtlStringLiteral::class.java)
        assertNotNull(literal)
        myFixture.editor.caretModel.moveToOffset(literal!!.textOffset + 2)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals(inc.virtualFile, ref?.resolve()?.containingFile?.virtualFile)
    }

    fun testMacroCallResolvesToDefinition() {
        myFixture.configureByText("main.ftl", "<#macro hello>\n<@hello>")
        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        val ident = call!!.identifier
        myFixture.editor.caretModel.moveToOffset(ident.textOffset + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals("hello", ref!!.element.text)
        val target = ref.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
    }

    fun testFunctionDeclarationResolvesFromMacroCall() {
        myFixture.configureByText("main.ftl", "<#function hello>\n<@hello>")
        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        val ident = call!!.identifier
        myFixture.editor.caretModel.moveToOffset(ident.textOffset + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals("hello", ref!!.element.text)
        val target = ref.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlFunctionDirective)
    }

    fun testSelfClosingMacroCallResolvesToDefinition() {
        myFixture.configureByText("main.ftl", "<#macro hello>\n<@hello/>")
        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        val ident = call!!.identifier
        myFixture.editor.caretModel.moveToOffset(ident.textOffset + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        assertEquals("hello", ref!!.element.text)
        val target = ref.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
    }
}
