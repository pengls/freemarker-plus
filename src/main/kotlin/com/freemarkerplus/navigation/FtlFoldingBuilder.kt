package com.freemarkerplus.navigation

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
 * 代码折叠：为成对的块级指令（if/list/switch/macro/function）生成折叠区间，
 * 范围从开启标签起点到闭合标签终点。用栈做配对以正确处理嵌套。
 */
class FtlFoldingBuilder : FoldingBuilder {

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val file = node.psi as? FtlFile ?: return emptyArray()

        val descriptors = mutableListOf<FoldingDescriptor>()
        val stack = ArrayDeque<Pair<String, ASTNode>>() // name -> open directive node

        PsiTreeUtil.processElements(file) { el ->
            when {
                el is FtlGenericDirective && el.isClosingDirective() ->
                    foldBlock(el.directiveName.text, el, stack, descriptors)
                el is FtlGenericDirective ->
                    pushOpen(el.directiveName.text, el.node, stack)
                el is FtlListDirective -> pushOpen("list", el.node, stack)
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
        private val FOLDABLE_NAMES = setOf("if", "list", "macro", "function", "switch")
    }
}
