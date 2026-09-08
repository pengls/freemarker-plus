package com.freemarkerplus.psi;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.freemarkerplus.psi.FtlElementTypes.*;

%%

%{
  // 指令内括号深度：> / < 只在括号内才是比较运算符，括号外 > 是指令结束符。
  // 每次从 YYINITIAL 进入 TAG 态时清零（见各定界符动作）。
  private int parenDepth = 0;

  public _FtlLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _FtlLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state TAG
%state INTERPOLATION
%state COMMENT

WHITE_SPACE=\s+

IDENT=[a-zA-Z_][a-zA-Z0-9_]*
STRING='([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\"
NUMBER=[0-9]+(\.[0-9]+)?
TEMPLATE_TEXT=([^<#$]|\$[^{]|\#[^{]|<[^#@/]|<\/[^#@])+

%%

// Template data: runs of data text (HTML/CSS/JS) that stop at FTL delimiters.
<YYINITIAL> {
  "<#--"                  { yybegin(COMMENT); return COMMENT_START; }
  "<#"                    { yybegin(TAG); parenDepth = 0; return OPEN_TAG; }
  "</#"                   { yybegin(TAG); parenDepth = 0; return CLOSE_TAG; }
  "<@"                    { yybegin(TAG); parenDepth = 0; return OPEN_MACRO; }
  "</@"                   { yybegin(TAG); parenDepth = 0; return CLOSE_MACRO; }
  "${"                    { yybegin(INTERPOLATION); return OPEN_INTERPOLATION; }
  "#{"                    { yybegin(INTERPOLATION); return OPEN_LEGACY; }
  {TEMPLATE_TEXT}          { return TEMPLATE_TEXT; }
}

// Inside <#...> / </#...> / <@...> / </@...>
<TAG> {
  {WHITE_SPACE}           { return WHITE_SPACE; }
  "include"               { return INCLUDE; }
  "import"                { return IMPORT; }
  "assign"                { return ASSIGN; }
  "local"                 { return LOCAL; }
  "global"                { return GLOBAL; }
  "macro"                 { return MACRO; }
  "function"              { return FUNCTION; }
  "list"                  { return LIST; }
  "if"                    { return IF; }
  "elseif"                { return ELSEIF; }
  "else"                  { return ELSE; }
  "switch"                { return SWITCH; }
  "case"                  { return CASE; }
  "default"               { return DEFAULT; }
  "break"                 { return BREAK; }
  "as"                    { return AS; }
  "??"                    { return QQ; }
  "?"                     { return QMARK; }
  ">="                    { if (parenDepth > 0) { return GE; } yybegin(YYINITIAL); parenDepth = 0; yypushback(1); return TAG_END; }
  "<="                    { if (parenDepth > 0) { return LE; } return BAD_CHARACTER; }
  ">"                     { if (parenDepth > 0) { return GT; } yybegin(YYINITIAL); parenDepth = 0; return TAG_END; }
  "<"                     { if (parenDepth > 0) { return LT; } return BAD_CHARACTER; }
  "."                     { return DOT; }
  ","                     { return COMMA; }
  "="                     { return ASSIGN_OP; }
  "("                     { parenDepth++; return LPAREN; }
  ")"                     { if (parenDepth > 0) { parenDepth--; } return RPAREN; }
  "/"                     { return SLASH; }
  {IDENT}                 { return IDENT; }
  {STRING}                { return STRING; }
  {NUMBER}                { return NUMBER; }
}

// Inside ${...} / #{...}
<INTERPOLATION> {
  {WHITE_SPACE}           { return WHITE_SPACE; }
  "}"                     { yybegin(YYINITIAL); return CLOSE_BRACE; }
  "??"                    { return QQ; }
  "?"                     { return QMARK; }
  ">="                    { return GE; }
  "<="                    { return LE; }
  ">"                     { return GT; }
  "<"                     { return LT; }
  "."                     { return DOT; }
  ","                     { return COMMA; }
  "="                     { return ASSIGN_OP; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }
  {IDENT}                 { return IDENT; }
  {STRING}                { return STRING; }
  {NUMBER}                { return NUMBER; }
}

// Inside <#-- ... --> (content is skipped, not tokenized)
<COMMENT> {
  "-->"                   { yybegin(YYINITIAL); return COMMENT_END; }
  [^]                     { /* skip */ }
}

[^] { return BAD_CHARACTER; }
