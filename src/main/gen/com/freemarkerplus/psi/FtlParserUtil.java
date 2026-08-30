package com.freemarkerplus.psi;

import com.intellij.lang.parser.GeneratedParserUtilBase;

/**
 * Parser utility base for the generated {@link FtlParser}. Grammar-Kit references
 * this class from {@code Freemarker.bnf}'s {@code parserUtilClass} attribute, but it
 * only generates it when the grammar defines custom utility methods. Our grammar
 * currently has none, so we provide an empty subclass by hand.
 */
public class FtlParserUtil extends GeneratedParserUtilBase {
}
