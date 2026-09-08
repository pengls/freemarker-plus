package com.freemarkerplus.lang

import com.intellij.lang.Commenter

/**
 * FTL 注释器：Ctrl+/（Comment with Block Comment）用 `<#-- -->` 包裹/解除选中区域。
 *
 * FreeMarker 只有块注释、没有行注释，故 [getLineCommentPrefix] 返回 null。
 * 后缀取 `-->`（不带前导空格）：FTL 中 `<#--x-->` 同样合法，且解除注释时
 * 能精确剥离我们包裹时写入的后缀。
 */
class FreemarkerCommenter : Commenter {
    override fun getLineCommentPrefix(): String? = null
    override fun getBlockCommentPrefix(): String = "<#--"
    override fun getBlockCommentSuffix(): String = "-->"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
