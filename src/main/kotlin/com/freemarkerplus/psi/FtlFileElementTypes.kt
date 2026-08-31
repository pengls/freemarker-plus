package com.freemarkerplus.psi

import com.freemarkerplus.lang.FreemarkerLanguage
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.OuterLanguageElementType

/**
 * 模板语言数据区元素类型（2026.2 平台机制，与官方 FTL 插件同款）。
 *
 * 数据文件（HTML 侧）的根内容元素是 [TEMPLATE_DATA] —— 一个懒展开的 chameleon：
 * 需要时 [TemplateDataElementType.parseContents] 用 FTL lexer 扫描全文，把
 * `TEMPLATE_TEXT` token（数据区文本）保留、其余（FTL 构造）挖洞为外语言元素，
 * 再用 HTML 解析器把保留的文本解析为真正的 HTML PSI —— 从而让平台的 HTML/JS
 * 导航（onclick → script 函数、script src 文件引用等）对 .ftl 生效。
 */
object FtlFileElementTypes {

    /** FTL 构造在数据文件里呈现为「外语言元素」（OuterLanguageElement）。 */
    val OUTER_ELEMENT_TYPE: IElementType =
        OuterLanguageElementType("FTL_FRAGMENT", FreemarkerLanguage.INSTANCE)

    /** 数据区 chameleon：templateElementType 是 FTL lexer 对数据区文本输出的 token。 */
    val TEMPLATE_DATA: TemplateDataElementType = object : TemplateDataElementType(
        "FTL_TEMPLATE_DATA",
        FreemarkerLanguage.INSTANCE,
        FtlElementTypes.TEMPLATE_TEXT,
        OUTER_ELEMENT_TYPE,
    ) {}
}
