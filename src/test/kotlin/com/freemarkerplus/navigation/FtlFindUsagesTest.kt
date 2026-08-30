package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlInterpolation
import com.freemarkerplus.psi.FtlMacroCall
import com.freemarkerplus.psi.FtlMacroDirective
import com.freemarkerplus.reference.FtlFileIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex

class FtlFindUsagesTest : BasePlatformTestCase() {

    fun testFindUsagesForMacro() {
        myFixture.configureByText("main.ftl", "<#macro m>a</#macro>\n<@m/>\n<@m/>")
        val macro = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroDirective::class.java)
        assertNotNull(macro)
        val usages = myFixture.findUsages(macro!!.identifier)
        assertEquals(2, usages.size)
    }

    fun testFindUsagesForVariable() {
        myFixture.configureByText("main.ftl", "<#assign user = \"a\">\n\${user}")
        val assign = PsiTreeUtil.findChildOfType(myFixture.file, FtlAssignDirective::class.java)
        assertNotNull(assign)
        val usages = myFixture.findUsages(assign!!.identifier)
        assertEquals(1, usages.size)
    }

    fun testFindUsagesAcrossFiles() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#macro m>a</#macro>")
        myFixture.configureByText("main.ftl", "<@m/>\n<@m/>")
        val macro = PsiTreeUtil.findChildOfType(lib, FtlMacroDirective::class.java)
        assertNotNull(macro)
        val usages = myFixture.findUsages(macro!!.identifier)
        assertEquals(2, usages.size)
    }

    fun testFindUsagesViaCaret() {
        myFixture.configureByText("main.ftl", "<#macro m>a</#macro>\n<@m<caret>/>\n<@m/>")
        val usages = myFixture.testFindUsages()
        assertEquals(2, usages.size)
    }

    fun testDefinitionSearcherAcrossFiles() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#macro hello>hi</#macro>")
        myFixture.configureByText("main.ftl", "<@hello/>")
        FileBasedIndex.getInstance()
            .ensureUpToDate(FtlFileIndex.NAME, myFixture.project, GlobalSearchScope.allScope(myFixture.project))
        val call = PsiTreeUtil.findChildOfType(myFixture.file, FtlMacroCall::class.java)
        assertNotNull(call)
        val defs = DefinitionsScopedSearch.search(call!!.identifier).findAll()
        assertEquals(1, defs.size)
        assertEquals("hello", defs.single().text)
        assertEquals(lib.virtualFile, defs.single().containingFile.virtualFile)
    }

    // 唯一能证明 FtlDefinitionSearcher 真正起作用的场景：跨文件变量定义。
    // FtlVariableReference 只在同文件内解析，${x}（main.ftl）找不到 lib.ftl 里的
    // <#assign x>；只有 definitionsSearch 能命中，故本测试专门走 searcher 路径。
    fun testDefinitionSearcherCrossFileVariable() {
        val lib = myFixture.addFileToProject("lib.ftl", "<#assign x = 1>")
        myFixture.configureByText("main.ftl", "\${x}")
        FileBasedIndex.getInstance()
            .ensureUpToDate(FtlFileIndex.NAME, myFixture.project, GlobalSearchScope.allScope(myFixture.project))
        val interp = PsiTreeUtil.findChildOfType(myFixture.file, FtlInterpolation::class.java)
        assertNotNull(interp)
        val usage = interp!!.expression.primaryList.first().identifier
        val defs = DefinitionsScopedSearch.search(usage).findAll()
        assertEquals(1, defs.size)
        assertEquals("x", defs.single().text)
        assertEquals(lib.virtualFile, defs.single().containingFile.virtualFile)
    }
}
