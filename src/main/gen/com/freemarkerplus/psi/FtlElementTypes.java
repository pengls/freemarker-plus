// This is a generated file. Not intended for manual editing.
package com.freemarkerplus.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.freemarkerplus.psi.impl.*;

public interface FtlElementTypes {

  IElementType ASSIGN_DIRECTIVE = new FtlElementType("ASSIGN_DIRECTIVE");
  IElementType ATTRIBUTE = new FtlElementType("ATTRIBUTE");
  IElementType COMMENT = new FtlElementType("COMMENT");
  IElementType DIRECTIVE_NAME = new FtlElementType("DIRECTIVE_NAME");
  IElementType EXPRESSION = new FtlElementType("EXPRESSION");
  IElementType FUNCTION_DIRECTIVE = new FtlElementType("FUNCTION_DIRECTIVE");
  IElementType GENERIC_DIRECTIVE = new FtlElementType("GENERIC_DIRECTIVE");
  IElementType IDENTIFIER = new FtlElementType("IDENTIFIER");
  IElementType IMPORT_DIRECTIVE = new FtlElementType("IMPORT_DIRECTIVE");
  IElementType INCLUDE_DIRECTIVE = new FtlElementType("INCLUDE_DIRECTIVE");
  IElementType INTERPOLATION = new FtlElementType("INTERPOLATION");
  IElementType LIST_DIRECTIVE = new FtlElementType("LIST_DIRECTIVE");
  IElementType MACRO_CALL = new FtlElementType("MACRO_CALL");
  IElementType MACRO_DIRECTIVE = new FtlElementType("MACRO_DIRECTIVE");
  IElementType OUTER_ELEMENT = new FtlElementType("OUTER_ELEMENT");
  IElementType PRIMARY = new FtlElementType("PRIMARY");
  IElementType STRING_LITERAL = new FtlElementType("STRING_LITERAL");

  IElementType AS = new FtlTokenType("as");
  IElementType ASSIGN = new FtlTokenType("assign");
  IElementType ASSIGN_OP = new FtlTokenType("=");
  IElementType BREAK = new FtlTokenType("break");
  IElementType CASE = new FtlTokenType("case");
  IElementType CLOSE_BRACE = new FtlTokenType("}");
  IElementType CLOSE_MACRO = new FtlTokenType("</@");
  IElementType CLOSE_TAG = new FtlTokenType("</#");
  IElementType COMMA = new FtlTokenType(",");
  IElementType COMMENT_END = new FtlTokenType("-->");
  IElementType COMMENT_START = new FtlTokenType("<#--");
  IElementType DEFAULT = new FtlTokenType("default");
  IElementType DOT = new FtlTokenType(".");
  IElementType ELSE = new FtlTokenType("else");
  IElementType ELSEIF = new FtlTokenType("elseif");
  IElementType FUNCTION = new FtlTokenType("function");
  IElementType GLOBAL = new FtlTokenType("global");
  IElementType IDENT = new FtlTokenType("IDENT");
  IElementType IF = new FtlTokenType("if");
  IElementType IMPORT = new FtlTokenType("import");
  IElementType INCLUDE = new FtlTokenType("include");
  IElementType LIST = new FtlTokenType("list");
  IElementType LOCAL = new FtlTokenType("local");
  IElementType LPAREN = new FtlTokenType("(");
  IElementType MACRO = new FtlTokenType("macro");
  IElementType NUMBER = new FtlTokenType("NUMBER");
  IElementType OPEN_INTERPOLATION = new FtlTokenType("${");
  IElementType OPEN_LEGACY = new FtlTokenType("#{");
  IElementType OPEN_MACRO = new FtlTokenType("<@");
  IElementType OPEN_TAG = new FtlTokenType("<#");
  IElementType RPAREN = new FtlTokenType(")");
  IElementType SLASH = new FtlTokenType("/");
  IElementType STRING = new FtlTokenType("STRING");
  IElementType SWITCH = new FtlTokenType("switch");
  IElementType TAG_END = new FtlTokenType(">");
  IElementType TEMPLATE_DATA = new FtlTokenType("TEMPLATE_DATA");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ASSIGN_DIRECTIVE) {
        return new FtlAssignDirectiveImpl(node);
      }
      else if (type == ATTRIBUTE) {
        return new FtlAttributeImpl(node);
      }
      else if (type == COMMENT) {
        return new FtlCommentImpl(node);
      }
      else if (type == DIRECTIVE_NAME) {
        return new FtlDirectiveNameImpl(node);
      }
      else if (type == EXPRESSION) {
        return new FtlExpressionImpl(node);
      }
      else if (type == FUNCTION_DIRECTIVE) {
        return new FtlFunctionDirectiveImpl(node);
      }
      else if (type == GENERIC_DIRECTIVE) {
        return new FtlGenericDirectiveImpl(node);
      }
      else if (type == IDENTIFIER) {
        return new FtlIdentifierImpl(node);
      }
      else if (type == IMPORT_DIRECTIVE) {
        return new FtlImportDirectiveImpl(node);
      }
      else if (type == INCLUDE_DIRECTIVE) {
        return new FtlIncludeDirectiveImpl(node);
      }
      else if (type == INTERPOLATION) {
        return new FtlInterpolationImpl(node);
      }
      else if (type == LIST_DIRECTIVE) {
        return new FtlListDirectiveImpl(node);
      }
      else if (type == MACRO_CALL) {
        return new FtlMacroCallImpl(node);
      }
      else if (type == MACRO_DIRECTIVE) {
        return new FtlMacroDirectiveImpl(node);
      }
      else if (type == OUTER_ELEMENT) {
        return new FtlOuterElementImpl(node);
      }
      else if (type == PRIMARY) {
        return new FtlPrimaryImpl(node);
      }
      else if (type == STRING_LITERAL) {
        return new FtlStringLiteralImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
