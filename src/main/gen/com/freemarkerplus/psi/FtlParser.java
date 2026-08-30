// This is a generated file. Not intended for manual editing.
package com.freemarkerplus.psi;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.freemarkerplus.psi.FtlElementTypes.*;
import static com.freemarkerplus.psi.FtlParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class FtlParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return ftlFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // COMMENT_START COMMENT_END
  public static boolean COMMENT(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "COMMENT")) return false;
    if (!nextTokenIs(builder_, COMMENT_START)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COMMENT_START, COMMENT_END);
    exit_section_(builder_, marker_, COMMENT, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_TAG (ASSIGN | LOCAL | GLOBAL) identifier ASSIGN_OP expression TAG_END
  public static boolean assign_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assign_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPEN_TAG);
    result_ = result_ && assign_directive_1(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ASSIGN_OP);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, ASSIGN_DIRECTIVE, result_);
    return result_;
  }

  // ASSIGN | LOCAL | GLOBAL
  private static boolean assign_directive_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assign_directive_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ASSIGN);
    if (!result_) result_ = consumeToken(builder_, LOCAL);
    if (!result_) result_ = consumeToken(builder_, GLOBAL);
    return result_;
  }

  /* ********************************************************** */
  // identifier ASSIGN_OP expression
  //                     | expression
  public static boolean attribute(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "attribute")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ATTRIBUTE, "<attribute>");
    result_ = attribute_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // identifier ASSIGN_OP expression
  private static boolean attribute_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "attribute_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ASSIGN_OP);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // IF | ELSEIF | ELSE | SWITCH | CASE | DEFAULT | BREAK | MACRO | FUNCTION
  //                     | LIST | ASSIGN | LOCAL | GLOBAL | IMPORT | INCLUDE | IDENT
  public static boolean directive_name(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directive_name")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIRECTIVE_NAME, "<directive name>");
    result_ = consumeToken(builder_, IF);
    if (!result_) result_ = consumeToken(builder_, ELSEIF);
    if (!result_) result_ = consumeToken(builder_, ELSE);
    if (!result_) result_ = consumeToken(builder_, SWITCH);
    if (!result_) result_ = consumeToken(builder_, CASE);
    if (!result_) result_ = consumeToken(builder_, DEFAULT);
    if (!result_) result_ = consumeToken(builder_, BREAK);
    if (!result_) result_ = consumeToken(builder_, MACRO);
    if (!result_) result_ = consumeToken(builder_, FUNCTION);
    if (!result_) result_ = consumeToken(builder_, LIST);
    if (!result_) result_ = consumeToken(builder_, ASSIGN);
    if (!result_) result_ = consumeToken(builder_, LOCAL);
    if (!result_) result_ = consumeToken(builder_, GLOBAL);
    if (!result_) result_ = consumeToken(builder_, IMPORT);
    if (!result_) result_ = consumeToken(builder_, INCLUDE);
    if (!result_) result_ = consumeToken(builder_, IDENT);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // primary (DOT primary)*
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXPRESSION, "<expression>");
    result_ = primary(builder_, level_ + 1);
    result_ = result_ && expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (DOT primary)*
  private static boolean expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "expression_1", pos_)) break;
    }
    return true;
  }

  // DOT primary
  private static boolean expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, DOT);
    result_ = result_ && primary(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // outer_element*
  static boolean ftlFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ftlFile")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!outer_element(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ftlFile", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // OPEN_TAG FUNCTION identifier attribute* TAG_END
  public static boolean function_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, OPEN_TAG, FUNCTION);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && function_directive_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, FUNCTION_DIRECTIVE, result_);
    return result_;
  }

  // attribute*
  private static boolean function_directive_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_directive_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "function_directive_3", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // OPEN_TAG directive_name attribute* TAG_END
  //                     | CLOSE_TAG directive_name TAG_END
  public static boolean generic_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "generic_directive")) return false;
    if (!nextTokenIs(builder_, "<generic directive>", CLOSE_TAG, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, GENERIC_DIRECTIVE, "<generic directive>");
    result_ = generic_directive_0(builder_, level_ + 1);
    if (!result_) result_ = generic_directive_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // OPEN_TAG directive_name attribute* TAG_END
  private static boolean generic_directive_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "generic_directive_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPEN_TAG);
    result_ = result_ && directive_name(builder_, level_ + 1);
    result_ = result_ && generic_directive_0_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // attribute*
  private static boolean generic_directive_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "generic_directive_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "generic_directive_0_2", pos_)) break;
    }
    return true;
  }

  // CLOSE_TAG directive_name TAG_END
  private static boolean generic_directive_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "generic_directive_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CLOSE_TAG);
    result_ = result_ && directive_name(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // IDENT
  public static boolean identifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "identifier")) return false;
    if (!nextTokenIs(builder_, IDENT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENT);
    exit_section_(builder_, marker_, IDENTIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_TAG IMPORT string_literal AS identifier TAG_END
  public static boolean import_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "import_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, OPEN_TAG, IMPORT);
    result_ = result_ && string_literal(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, AS);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, IMPORT_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_TAG INCLUDE string_literal TAG_END
  public static boolean include_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "include_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, OPEN_TAG, INCLUDE);
    result_ = result_ && string_literal(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, INCLUDE_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_INTERPOLATION expression CLOSE_BRACE
  //                     | OPEN_LEGACY expression CLOSE_BRACE
  public static boolean interpolation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interpolation")) return false;
    if (!nextTokenIs(builder_, "<interpolation>", OPEN_INTERPOLATION, OPEN_LEGACY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INTERPOLATION, "<interpolation>");
    result_ = interpolation_0(builder_, level_ + 1);
    if (!result_) result_ = interpolation_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // OPEN_INTERPOLATION expression CLOSE_BRACE
  private static boolean interpolation_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interpolation_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPEN_INTERPOLATION);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, CLOSE_BRACE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // OPEN_LEGACY expression CLOSE_BRACE
  private static boolean interpolation_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interpolation_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPEN_LEGACY);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, CLOSE_BRACE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_TAG LIST expression AS identifier TAG_END
  public static boolean list_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "list_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, OPEN_TAG, LIST);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, AS);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, LIST_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_MACRO identifier attribute* SLASH? TAG_END
  //                    | CLOSE_MACRO identifier TAG_END
  public static boolean macro_call(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_call")) return false;
    if (!nextTokenIs(builder_, "<macro call>", CLOSE_MACRO, OPEN_MACRO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, MACRO_CALL, "<macro call>");
    result_ = macro_call_0(builder_, level_ + 1);
    if (!result_) result_ = macro_call_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // OPEN_MACRO identifier attribute* SLASH? TAG_END
  private static boolean macro_call_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_call_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPEN_MACRO);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && macro_call_0_2(builder_, level_ + 1);
    result_ = result_ && macro_call_0_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // attribute*
  private static boolean macro_call_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_call_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "macro_call_0_2", pos_)) break;
    }
    return true;
  }

  // SLASH?
  private static boolean macro_call_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_call_0_3")) return false;
    consumeToken(builder_, SLASH);
    return true;
  }

  // CLOSE_MACRO identifier TAG_END
  private static boolean macro_call_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_call_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CLOSE_MACRO);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_TAG MACRO identifier attribute* TAG_END
  public static boolean macro_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_directive")) return false;
    if (!nextTokenIs(builder_, OPEN_TAG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, OPEN_TAG, MACRO);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && macro_directive_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TAG_END);
    exit_section_(builder_, marker_, MACRO_DIRECTIVE, result_);
    return result_;
  }

  // attribute*
  private static boolean macro_directive_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macro_directive_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "macro_directive_3", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // COMMENT | include_directive | import_directive | assign_directive
  //                    | macro_directive | function_directive | list_directive
  //                    | macro_call | interpolation | generic_directive | TEMPLATE_DATA
  public static boolean outer_element(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "outer_element")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OUTER_ELEMENT, "<outer element>");
    result_ = COMMENT(builder_, level_ + 1);
    if (!result_) result_ = include_directive(builder_, level_ + 1);
    if (!result_) result_ = import_directive(builder_, level_ + 1);
    if (!result_) result_ = assign_directive(builder_, level_ + 1);
    if (!result_) result_ = macro_directive(builder_, level_ + 1);
    if (!result_) result_ = function_directive(builder_, level_ + 1);
    if (!result_) result_ = list_directive(builder_, level_ + 1);
    if (!result_) result_ = macro_call(builder_, level_ + 1);
    if (!result_) result_ = interpolation(builder_, level_ + 1);
    if (!result_) result_ = generic_directive(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, TEMPLATE_DATA);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  //                     | string_literal
  //                     | NUMBER
  //                     | LPAREN expression RPAREN
  public static boolean primary(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primary")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRIMARY, "<primary>");
    result_ = identifier(builder_, level_ + 1);
    if (!result_) result_ = string_literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, NUMBER);
    if (!result_) result_ = primary_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // LPAREN expression RPAREN
  private static boolean primary_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primary_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LPAREN);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // STRING
  public static boolean string_literal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "string_literal")) return false;
    if (!nextTokenIs(builder_, STRING)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, STRING);
    exit_section_(builder_, marker_, STRING_LITERAL, result_);
    return result_;
  }

}
