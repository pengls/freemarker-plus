package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlInterpolation
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroCall
import com.freemarkerplus.psi.FtlMacroDirective
import com.freemarkerplus.psi.FtlStringLiteral
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex

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

    fun testNamespaceResolvesAcrossFiles() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#macro hello>hi</#macro>")
        myFixture.configureByText("main.ftl", "<#import \"lib.ftl\" as lib>\n\${lib.hello}")
        val interpolation = PsiTreeUtil.findChildOfType(myFixture.file, FtlInterpolation::class.java)
        assertNotNull(interpolation)
        val rootIdent = interpolation!!.expression.primaryList.first().identifier
        assertNotNull(rootIdent)
        assertEquals("lib", rootIdent!!.text)
        // 根段 lib 上同时挂了变量引用与命名空间引用；取命名空间引用验证跨文件跳转。
        val nsRef = rootIdent.references.filterIsInstance<FtlNamespaceReference>().firstOrNull()
        assertNotNull("expected a namespace reference on the lib root segment", nsRef)
        val targets = nsRef!!.multiResolve(false)
        assertTrue("namespace reference should resolve to the hello macro", targets.isNotEmpty())
        val target = targets.first().element
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
        assertEquals(lib.virtualFile, target.containingFile.virtualFile)
    }

    fun testNamespaceResolvesViaFindReferenceAt() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#macro hello>hi</#macro>")
        myFixture.configureByText("main.ftl", "<#import \"lib.ftl\" as lib>\n\${lib.hello}")
        // caret 到根段 lib，走 Ctrl+B 的真实入口 findReferenceAt；findReferenceAt 会返回一个
        // 包裹根段所有引用的 PsiMultiReference，其 resolve() 取第一个匹配（命名空间引用在前）。
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("lib.hello") + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
        assertEquals(lib.virtualFile, target.containingFile.virtualFile)
    }

    fun testMacroCallResolvesAcrossFiles() {
        val other = myFixture.addFileToProject("other.ftl", "<#macro hello>hi</#macro>")
        FileBasedIndex.getInstance()
            .ensureUpToDate(FtlFileIndex.NAME, myFixture.project, GlobalSearchScope.allScope(myFixture.project))
        myFixture.configureByText("main.ftl", "<@hello>")
        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        val ident = call!!.identifier
        myFixture.editor.caretModel.moveToOffset(ident.textOffset + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
        assertEquals(other.virtualFile, target.containingFile.virtualFile)
    }

    fun testMacroParameterResolvesFromBody() {
        myFixture.configureByText("main.ftl", "<#macro greet name>Hello \${name}</#macro>")
        // 宏体内的 ${name} → 解析到宏参数 name
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.lastIndexOf("name"))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("name", target!!.text)
        assertTrue(PsiTreeUtil.getParentOfType(target, FtlMacroDirective::class.java) != null)
    }

    fun testMacroParameterWithDefaultResolves() {
        myFixture.configureByText("main.ftl", "<#macro box cols=3>\${cols}</#macro>")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.lastIndexOf("cols"))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("cols", target!!.text)
    }

    fun testNamespaceResolvesMultiLevelImportPath() {
        val lib = myFixture.addFileToProject("partials/lib.ftl", "<#macro hello>hi</#macro>")
        myFixture.configureByText("main.ftl", "<#import \"partials/lib.ftl\" as lib>\n\${lib.hello}")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("lib.hello") + 1)
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("hello", target!!.text)
        assertTrue(target.parent is FtlMacroDirective)
        assertEquals(lib.virtualFile, target.containingFile.virtualFile)
    }

    fun testRootSegmentBeforeBuiltinResolves() {
        // 注意：变量名避开 TAG 态关键字（如 list/assign），关键字抢占 IDENT 是已知边缘 case
        myFixture.configureByText("main.ftl", "<#assign items = 1>\n\${items?size}")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("items", myFixture.file.text.indexOf("\${")))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("items", target!!.text)
        assertTrue(target.parent is FtlAssignDirective)
    }

    fun testBuiltinNameIsNotVariableReference() {
        myFixture.configureByText("main.ftl", "<#assign items = 1>\n\${items?size}")
        // ? 后的内建函数名不是变量：不挂变量引用，避免误标红/误跳转
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("size", myFixture.file.text.indexOf("\${")))
        assertNull(myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset))
    }

    fun testExistsOperatorRootResolves() {
        myFixture.configureByText("main.ftl", "<#assign user = 1>\n<#if user??>")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("user", myFixture.file.text.indexOf("<#if ")))
        val ref = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
        assertNotNull(ref)
        val target = ref!!.resolve()
        assertNotNull(target)
        assertEquals("user", target!!.text)
        assertTrue(target.parent is FtlAssignDirective)
    }
}
