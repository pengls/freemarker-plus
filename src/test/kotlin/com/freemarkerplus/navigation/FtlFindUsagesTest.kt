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

    // 重命名声明名：声明与所有引用处同步改名。
    fun testRenameMacro() {
        myFixture.configureByText("main.ftl", "<#macro m<caret>>a</#macro>\n<@m/>\n<@m/>")
        myFixture.renameElementAtCaret("m2")
        assertEquals("<#macro m2>a</#macro>\n<@m2/>\n<@m2/>", myFixture.file.text)
    }

    // 重命名引用处（解析到声明后）同样应同步改名声明与所有引用处。
    fun testRenameMacroFromUsage() {
        myFixture.configureByText("main.ftl", "<#macro m>a</#macro>\n<@m<caret>/>\n<@m/>")
        myFixture.renameElementAtCaret("m2")
        assertEquals("<#macro m2>a</#macro>\n<@m2/>\n<@m2/>", myFixture.file.text)
    }

    fun testRenameVariable() {
        myFixture.configureByText("main.ftl", "<#assign user<caret> = \"a\">\n\${user}\n\${user}")
        myFixture.renameElementAtCaret("u2")
        assertEquals("<#assign u2 = \"a\">\n\${u2}\n\${u2}", myFixture.file.text)
    }

    // 重命名 import 别名应被拒绝：别名没有指向自身的引用（lib.member 解析到被导入文件的
    // 成员声明），改名只会静默脱钩所有 lib.member 使用处。canProcessElement 返回 false
    // 后重命名应为 no-op，文件文本保持不变。
    fun testRenameImportAliasIsRejected() {
        myFixture.configureByText("main.ftl", "<#import \"lib.ftl\" as li<caret>b>")
        myFixture.renameElementAtCaret("lib2")
        assertEquals("<#import \"lib.ftl\" as lib>", myFixture.file.text)
    }
}
