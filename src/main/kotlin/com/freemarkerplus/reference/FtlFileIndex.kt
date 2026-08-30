package com.freemarkerplus.reference

import com.freemarkerplus.psi.FtlAssignDirective
import com.freemarkerplus.psi.FtlFile
import com.freemarkerplus.psi.FtlFunctionDirective
import com.freemarkerplus.psi.FtlListDirective
import com.freemarkerplus.psi.FtlMacroDirective
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

/** 一个声明（宏/函数/变量/循环变量）的索引条目。 */
data class FtlIndexInfo(val name: String, val type: String, val filePath: String)

/**
 * 文件级声明索引：按名字映射到各文件里的宏/函数/变量声明，供跨文件跳转使用
 * （例如 main.ftl 里裸的 `<@hello>` 命中 other.ftl 里的 `<#macro hello>`）。
 */
class FtlFileIndex : FileBasedIndexExtension<String, List<FtlIndexInfo>>() {

    override fun getName(): ID<String, List<FtlIndexInfo>> = NAME

    override fun getVersion(): Int = 1

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<List<FtlIndexInfo>> =
        object : DataExternalizer<List<FtlIndexInfo>> {
            override fun save(out: DataOutput, value: List<FtlIndexInfo>) {
                out.writeInt(value.size)
                value.forEach {
                    out.writeUTF(it.name)
                    out.writeUTF(it.type)
                    out.writeUTF(it.filePath)
                }
            }

            override fun read(input: DataInput): List<FtlIndexInfo> {
                val n = input.readInt()
                return (0 until n).map {
                    FtlIndexInfo(input.readUTF(), input.readUTF(), input.readUTF())
                }
            }
        }

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension == "ftl" || file.extension == "ftlh" || file.extension == "ftlx"
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, List<FtlIndexInfo>, FileContent> =
        DataIndexer { inputData ->
            val result = HashMap<String, MutableList<FtlIndexInfo>>()
            val psiFile = inputData.psiFile as? FtlFile
            if (psiFile != null) {
                collectDeclarations(psiFile, inputData.file.path).forEach { info ->
                    result.getOrPut(info.name) { mutableListOf() }.add(info)
                }
            }
            result
        }

    // 遍历 PSI 树收集声明，与 FtlPsiUtil 的收集逻辑一致。
    private fun collectDeclarations(file: FtlFile, path: String): List<FtlIndexInfo> {
        val out = mutableListOf<FtlIndexInfo>()
        PsiTreeUtil.processElements(file) { el ->
            when (el) {
                is FtlMacroDirective -> out.add(FtlIndexInfo(el.identifier.text, "macro", path))
                is FtlFunctionDirective -> out.add(FtlIndexInfo(el.identifier.text, "function", path))
                is FtlAssignDirective -> out.add(FtlIndexInfo(el.identifier.text, "variable", path))
                is FtlListDirective -> out.add(FtlIndexInfo(el.identifier.text, "loop", path))
                else -> {}
            }
            true
        }
        return out
    }

    companion object {
        val NAME: ID<String, List<FtlIndexInfo>> = ID.create("com.freemarkerplus.ftlDeclarations")
    }
}
