package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlElementTypes
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlGenericDirective
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroDirective
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil

/**
 * 代码折叠：为成对的块级指令（if/list/switch/macro/function/compress/noparse/escape）
 * 生成折叠区间，范围从开启标签起点到闭合标签终点。用栈做配对以正确处理嵌套。
 * 另为 `<#-- ... -->` 注释提供整体折叠。
 */
class FtlFoldingBuilder : FoldingBuilder {

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val file = node.psi as? FtlFile ?: return emptyArray()

        val descriptors = mutableListOf<FoldingDescriptor>()
        val stack = ArrayDeque<Pair<String, ASTNode>>() // name -> open directive node
        // 注释在 PSI 中被拆成 COMMENT_START / COMMENT_END 两个叶子（getCommentTokens 机制，
        // 注释内容不进 PSI），按「起始叶→结束叶」配对折叠，区间覆盖中间的注释内容。
        var commentStart: ASTNode? = null

        PsiTreeUtil.processElements(file) { el ->
            when {
                el.node.elementType == FtlElementTypes.COMMENT_START ->
                    if (commentStart == null) commentStart = el.node
                el.node.elementType == FtlElementTypes.COMMENT_END -> {
                    val start = commentStart
                    if (start != null) {
                        val range = TextRange(start.startOffset, el.node.startOffset + el.node.textLength)
                        // 占位符从全文截取（COMMENT_START 叶子本身只有 "<#--"）
                        val text = file.text.substring(range.startOffset, range.endOffset)
                        val firstLine = text.lines().first().trim()
                        val placeholder = if (text.contains('\n')) "$firstLine … -->" else firstLine
                        descriptors += FoldingDescriptor(start, range, null, placeholder)
                        commentStart = null
                    }
                }
                el is FtlGenericDirective && el.isClosingDirective() ->
                    foldBlock(el.directiveName.text, el, stack, descriptors)
                el is FtlGenericDirective ->
                    pushOpen(el.directiveName.text, el.node, stack)
                el is FtlListDirective -> pushOpen("list", el.node, stack)
                el is FtlAssignDirective -> pushOpen(assignBlockName(el), el.node, stack)
                el is FtlMacroDirective -> pushOpen("macro", el.node, stack)
                el is FtlFunctionDirective -> pushOpen("function", el.node, stack)
            }
            true
        }
        return descriptors.toTypedArray()
    }

    private fun pushOpen(name: String, node: ASTNode, stack: ArrayDeque<Pair<String, ASTNode>>) {
        if (name in FOLDABLE_NAMES) stack.addLast(name to node)
    }

    // 块级赋值指令的关键字（<#assign>/<#local>/<#global>），与闭合标签文本配对
    private fun assignBlockName(el: FtlAssignDirective): String =
        el.node.getChildren(null).firstOrNull {
            it.elementType == FtlElementTypes.ASSIGN ||
                it.elementType == FtlElementTypes.LOCAL ||
                it.elementType == FtlElementTypes.GLOBAL
        }?.text ?: "assign"

    private fun foldBlock(
        name: String,
        close: FtlGenericDirective,
        stack: ArrayDeque<Pair<String, ASTNode>>,
        descriptors: MutableList<FoldingDescriptor>,
    ) {
        val idx = stack.indexOfLast { it.first == name }
        if (idx < 0) return
        val (_, openNode) = stack[idx]
        while (stack.size > idx) stack.removeLast()
        descriptors += FoldingDescriptor(openNode, TextRange(openNode.textRange.startOffset, close.textRange.endOffset))
    }

    override fun getPlaceholderText(node: ASTNode): String =
        node.text.lines().firstOrNull()?.trim() ?: node.text

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    companion object {
        private val FOLDABLE_NAMES =
            setOf(
                "if", "list", "macro", "function", "switch", "compress", "noparse", "escape",
                "assign", "local", "global",
            )
    }
}
