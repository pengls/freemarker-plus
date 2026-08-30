package com.freemarkerplus.psi;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.freemarkerplus.psi.FtlElementTypes.*;

%%

%{
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

EOL=\R
WHITE_SPACE=\s+

IDENT=[a-zA-Z_][a-zA-Z0-9_]*
STRING='([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\"
NUMBER=[0-9]+(\.[0-9]+)?
TEMPLATE_DATA=([^<]|<[^#@/])+

%%
<YYINITIAL> {
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
  "<#"                    { return OPEN_TAG; }
  "</#"                   { return CLOSE_TAG; }
  "<@"                    { return OPEN_MACRO; }
  "</@"                   { return CLOSE_MACRO; }
  "${"                    { return OPEN_INTERPOLATION; }
  "#{"                    { return OPEN_LEGACY; }
  "}"                     { return CLOSE_BRACE; }
  ">"                     { return TAG_END; }
  "<#--"                  { return COMMENT_START; }
  "-->"                   { return COMMENT_END; }
  "."                     { return DOT; }
  ","                     { return COMMA; }
  "="                     { return ASSIGN_OP; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }

  {IDENT}                 { return IDENT; }
  {STRING}                { return STRING; }
  {NUMBER}                { return NUMBER; }
  {TEMPLATE_DATA}         { return TEMPLATE_DATA; }
}

[^] { return BAD_CHARACTER; }
