package com.freemarkerplus.reference

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
}
