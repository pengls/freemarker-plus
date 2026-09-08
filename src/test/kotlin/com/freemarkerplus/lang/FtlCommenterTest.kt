package com.freemarkerplus.lang

import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlCommenterTest : BasePlatformTestCase() {

    fun testBlockCommenterRegisteredForLanguage() {
        val commenter = LanguageCommenters.INSTANCE.forLanguage(FreemarkerLanguage.INSTANCE)
        val ftl = commenter as? FreemarkerCommenter
        assertNotNull("expected FreemarkerCommenter, got ${commenter?.javaClass}", ftl)
        assertEquals("<#--", ftl!!.blockCommentPrefix)
        assertEquals("-->", ftl.blockCommentSuffix)
        assertNull(ftl.lineCommentPrefix)
    }

    fun testBlockCommentActionWrapsSelection() {
        myFixture.configureByText("main.ftl", "<div>\${user}</div>")
        val start = myFixture.file.text.indexOf("\${user}")
        // 编辑器动作依赖当前 caret 状态：光标先移入选区
        myFixture.editor.caretModel.moveToOffset(start)
        myFixture.editor.selectionModel.setSelection(start, start + "\${user}".length)
        val action = com.intellij.openapi.actionSystem.ActionManager.getInstance()
            .getAction(IdeActions.ACTION_COMMENT_BLOCK)!!
        val presentation = myFixture.testAction(action)
        assertTrue("action should be enabled", presentation.isEnabled && presentation.isVisible)
        // 动作写入 Document 后需提交 PSI，file.text 才会反映变更
        com.intellij.psi.PsiDocumentManager.getInstance(myFixture.project)
            .commitDocument(myFixture.editor.document)
        assertTrue(
            "expected FTL block comment, got: " + myFixture.file.text,
            myFixture.file.text.contains("<#--\${user}-->")
        )
    }
}
