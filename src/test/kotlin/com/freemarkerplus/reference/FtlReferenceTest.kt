package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlListDirective
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

    fun testVariableResolvesToAssign() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n\${user}")
        // caret 移到 ${user} 的 user
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("user", myFixture.file.text.indexOf("\${")))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("user", target!!.text)
        assertTrue(target.parent is FtlAssignDirective)
    }

    fun testVariableResolvesToListLoopVar() {
        myFixture.configureByText("main.ftl", "<#list items as item>\n\${item}")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("item", myFixture.file.text.indexOf("\${")))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("item", target!!.text)
        assertTrue(target.parent is FtlListDirective)
    }

    fun testVariableResolvesInsideDirective() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n<#if user>")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("user", myFixture.file.text.indexOf("<#if ")))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("user", target!!.text)
        assertTrue(target.parent is FtlAssignDirective)
    }

    fun testDottedExpressionOnlyRootSegmentResolves() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n\${user.name}")
        // 根段 user → 解析到声明
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("user", myFixture.file.text.indexOf("\${")))
        val rootRef = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(rootRef)
        assertEquals("user", rootRef!!.element.text)
        assertNotNull(rootRef.resolve())
        // 属性段 name 属 Phase 3 Java 数据模型，不应挂变量引用
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("name", myFixture.file.text.indexOf("\${")))
        assertNull(myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset))
    }
}
