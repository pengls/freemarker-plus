package com.freemarkerplus.navigation

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.lang.LanguageNamesValidation
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FtlNamesValidatorTest : BasePlatformTestCase() {

    // 校验 lang.namesValidator 已为 FTL 注册，且按 FreeMarker 标识符规则（IDENT lexer 正则）
    // 拒绝含 '-' 或空格、以数字开头的非法名。
    fun testNamesValidatorRejectsInvalidIdentifiers() {
        val validator = LanguageNamesValidation.INSTANCE.forLanguage(FreemarkerLanguage.INSTANCE)
        assertNotNull("namesValidator should be registered for FTL", validator)
        assertTrue(validator.isIdentifier("myMacro", project))
        assertTrue(validator.isIdentifier("_private", project))
        assertTrue(validator.isIdentifier("a1", project))
        assertFalse(validator.isIdentifier("my-macro", project))
        assertFalse(validator.isIdentifier("my macro", project))
        assertFalse(validator.isIdentifier("1abc", project))
    }
}
