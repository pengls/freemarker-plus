package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlAssignDirective
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
}
