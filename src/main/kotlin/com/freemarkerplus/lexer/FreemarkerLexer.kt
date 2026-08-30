package com.freemarkerplus.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class FreemarkerLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var pos = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    private enum class Mode { DATA, INTERPOLATION, TAG, STYLE, SCRIPT }

    private var mode = Mode.DATA
    private var expectName = false

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.pos = startOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        this.mode = Mode.DATA
        this.expectName = false
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferEnd(): Int = endOffset

    override fun getBufferSequence(): CharSequence = buffer

    override fun advance() {
        tokenStart = pos
        if (pos >= endOffset) {
            tokenType = null
            tokenEnd = pos
            return
        }
        when (mode) {
            Mode.DATA -> advanceData()
            Mode.INTERPOLATION -> advanceInterpolation()
            Mode.TAG -> advanceTag()
            Mode.STYLE -> advanceEmbeddedContent("style", FreemarkerTokenTypes.STYLE_DATA)
            Mode.SCRIPT -> advanceEmbeddedContent("script", FreemarkerTokenTypes.SCRIPT_DATA)
        }
    }

    private fun advanceData() {
        when {
            startsWithIgnoreCase(pos, "<style") -> lexEmbeddedTagStart(Mode.STYLE)
            startsWithIgnoreCase(pos, "<script") -> lexEmbeddedTagStart(Mode.SCRIPT)
            startsWith(pos, "<#--") -> {
                tokenType = FreemarkerTokenTypes.COMMENT
                val end = indexOf(pos, "-->")
                pos = if (end >= 0) end + 3 else endOffset
                tokenEnd = pos
            }
            startsWith(pos, "</#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 3
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "</@") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 3
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<#") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "<@") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.TAG
                expectName = true
            }
            startsWith(pos, "\${") -> {
                tokenType = FreemarkerTokenTypes.INTERPOLATION
                pos += 2
                tokenEnd = pos
                mode = Mode.INTERPOLATION
            }
            else -> lexPlainData()
        }
    }

    private fun lexPlainData() {
        while (pos < endOffset) {
            val c = buffer[pos]
            if (c == '\\' && pos + 1 < endOffset && (buffer[pos + 1] == '$' || buffer[pos + 1] == '<')) {
                pos += 2
                continue
            }
            if (isFreemarkerStart(pos)) break
            if (startsWithIgnoreCase(pos, "<style") || startsWithIgnoreCase(pos, "<script")) break
            pos++
        }
        tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
        tokenEnd = pos
    }

    private fun lexEmbeddedTagStart(contentMode: Mode) {
        val gt = indexOf(pos, ">")
        if (gt >= 0) {
            val selfClosing = gt > pos && buffer[gt - 1] == '/'
            pos = gt + 1
            tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
            tokenEnd = pos
            mode = if (selfClosing) Mode.DATA else contentMode
        } else {
            pos = endOffset
            tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
            tokenEnd = pos
        }
    }

    private fun advanceEmbeddedContent(tagName: String, dataType: IElementType) {
        val close = indexOfIgnoreCase(pos, "</$tagName")
        if (close < 0) {
            tokenType = dataType
            tokenEnd = endOffset
            pos = endOffset
        } else if (close == pos) {
            // Empty content (e.g. <script src="x.js"></script>): emit the closing
            // tag as HTML data instead of a zero-length data token.
            tokenType = FreemarkerTokenTypes.TEMPLATE_DATA
            val gt = indexOf(close, ">")
            pos = if (gt >= 0) gt + 1 else endOffset
            tokenEnd = pos
        } else {
            tokenType = dataType
            tokenEnd = close
            pos = close
        }
        mode = Mode.DATA
    }

    private fun startsWithIgnoreCase(offset: Int, text: String): Boolean {
        if (offset + text.length > endOffset) return false
        for (i in text.indices) {
            if (buffer[offset + i].lowercaseChar() != text[i].lowercaseChar()) return false
        }
        return true
    }

    private fun indexOfIgnoreCase(from: Int, text: String): Int {
        var i = from
        while (i <= endOffset - text.length) {
            if (startsWithIgnoreCase(i, text)) return i
            i++
        }
        return -1
    }

    private fun advanceInterpolation() {
        if (buffer[pos] == '}') {
            tokenType = FreemarkerTokenTypes.INTERPOLATION
            pos++
            tokenEnd = pos
            mode = Mode.DATA
            return
        }
        lexExpressionToken()
    }

    private fun advanceTag() {
        if (buffer[pos] == '>') {
            tokenType = FreemarkerTokenTypes.INTERPOLATION
            pos++
            tokenEnd = pos
            mode = Mode.DATA
            return
        }
        if (expectName) {
            if (buffer[pos].isWhitespace()) {
                lexWhitespace()
                return
            }
            if (buffer[pos].isJavaIdentifierStart()) {
                while (pos < endOffset && buffer[pos].isJavaIdentifierPart()) pos++
                tokenType = FreemarkerTokenTypes.DIRECTIVE_NAME
                tokenEnd = pos
                expectName = false
                return
            }
            pos++
            tokenType = FreemarkerTokenTypes.BAD_CHARACTER
            tokenEnd = pos
            return
        }
        lexExpressionToken()
    }

    private fun lexExpressionToken() {
        tokenStart = pos
        val c = buffer[pos]
        when {
            c.isWhitespace() -> lexWhitespace()
            c == '"' || c == '\'' -> lexString(c)
            c.isDigit() -> lexNumber()
            c.isJavaIdentifierStart() -> lexIdentifier()
            else -> {
                pos++
                tokenType = FreemarkerTokenTypes.OPERATOR
                tokenEnd = pos
            }
        }
    }

    private fun lexWhitespace() {
        while (pos < endOffset && buffer[pos].isWhitespace()) pos++
        tokenType = TokenType.WHITE_SPACE
        tokenEnd = pos
    }

    private fun lexString(quote: Char) {
        pos++
        while (pos < endOffset) {
            if (buffer[pos] == '\\' && pos + 1 < endOffset) {
                pos += 2
                continue
            }
            if (buffer[pos] == quote) {
                pos++
                break
            }
            pos++
        }
        tokenType = FreemarkerTokenTypes.STRING
        tokenEnd = pos
    }

    private fun lexNumber() {
        while (pos < endOffset && buffer[pos].isDigit()) pos++
        if (pos + 1 < endOffset && buffer[pos] == '.' && buffer[pos + 1].isDigit()) {
            pos++
            while (pos < endOffset && buffer[pos].isDigit()) pos++
        }
        tokenType = FreemarkerTokenTypes.NUMBER
        tokenEnd = pos
    }

    private fun lexIdentifier() {
        while (pos < endOffset && buffer[pos].isJavaIdentifierPart()) pos++
        val text = buffer.subSequence(tokenStart, pos).toString()
        tokenType = if (text in KEYWORDS) FreemarkerTokenTypes.KEYWORD else FreemarkerTokenTypes.IDENTIFIER
        tokenEnd = pos
    }

    private fun isFreemarkerStart(offset: Int): Boolean =
        startsWith(offset, "\${") || startsWith(offset, "<#") || startsWith(offset, "<@") ||
        startsWith(offset, "</#") || startsWith(offset, "</@")

    private fun startsWith(offset: Int, text: String): Boolean {
        if (offset + text.length > endOffset) return false
        for (i in text.indices) {
            if (buffer[offset + i] != text[i]) return false
        }
        return true
    }

    private fun indexOf(from: Int, text: String): Int {
        if (from >= endOffset) return -1
        var i = from
        while (i <= endOffset - text.length) {
            if (startsWith(i, text)) return i
            i++
        }
        return -1
    }

    companion object {
        private val KEYWORDS = setOf(
            "true", "false", "and", "or", "not", "gt", "gte", "lt", "lte",
            "if", "else", "elseif", "list", "break", "continue",
            "include", "import", "assign", "local", "global",
            "macro", "function", "return", "switch", "case", "default",
            "as", "in", "using", "new"
        )
    }
}
