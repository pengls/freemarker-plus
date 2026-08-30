package com.freemarkerplus.navigation

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlElementTypes
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlGenericDirective
import com.freemarkerplus.psi.FtlImportDirective
import com.freemarkerplus.psi.FtlIncludeDirective
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroCall
import com.freemarkerplus.psi.FtlMacroDirective
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

/**
 * Alt+7 结构视图：以文件为根，平铺展示模板中的指令/宏调用（开启标签）。
 *
 * PSI 树是扁平的（outer_element 之间不嵌套），因此结构视图按文档顺序平铺；
 * 每个结构元素都可导航（点击跳转到编辑器对应指令位置）。
 */
class FtlStructureViewModel(psiFile: PsiFile, editor: Editor?) :
    TextEditorBasedStructureViewModel(editor, psiFile) {

    override fun getRoot(): StructureViewTreeElement =
        FtlStructureElement(psiFile, psiFile.name, collectDirectives(psiFile))

    private fun collectDirectives(scope: PsiElement): List<StructureViewTreeElement> {
        val result = mutableListOf<StructureViewTreeElement>()
        PsiTreeUtil.processElements(scope) { el ->
            if (el.isStructuralDirective()) {
                result += FtlStructureElement(el, el.openingTagText())
            }
            true
        }
        return result
    }
}

// 是否为「结构性」指令节点：块级/独立指令与宏调用的开启标签，不含闭合标签与 outer_element 包装节点。
internal fun PsiElement.isStructuralDirective(): Boolean = when (this) {
    is FtlGenericDirective, is FtlMacroCall -> !isClosingDirective()
    is FtlMacroDirective, is FtlFunctionDirective, is FtlAssignDirective,
    is FtlListDirective, is FtlIncludeDirective, is FtlImportDirective -> true
    else -> false
}

// 是否为闭合标签（</#x> 或 </@x>）。
internal fun PsiElement.isClosingDirective(): Boolean =
    firstChild?.node?.elementType.let { it == FtlElementTypes.CLOSE_TAG || it == FtlElementTypes.CLOSE_MACRO }

// 开启标签的展示文本（首行，去除首尾空白）。
internal fun PsiElement.openingTagText(): String =
    text.lines().firstOrNull()?.trim() ?: text

private class FtlStructureElement(
    private val element: PsiElement,
    private val label: String,
    private val children: List<TreeElement> = emptyList(),
) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = label
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun getChildren(): Array<TreeElement> = children.toTypedArray()

    override fun navigate(requestFocus: Boolean) {
        (element as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean =
        (element as? Navigatable)?.canNavigate() == true

    override fun canNavigateToSource(): Boolean = canNavigate()
}
