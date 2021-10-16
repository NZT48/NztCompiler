package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.List;
import rs.ac.bg.etf.pp1.CompilerError.CompilerErrorType;

%%

%{
	
	List<CompilerError> errors = new ArrayList<>();

	public List<CompilerError> getLexicalErrors(){
    	return errors;
    }

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}
	
	private void report_error(String message, int line) {
		System.err.println(message);
		CompilerError error = new CompilerError(line, message, CompilerErrorType.LEXICAL_ERROR);
		errors.add(error);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }
"\n"    { }

"program"   { return new_symbol(sym.PROG, yytext());}
"break" 	{ return new_symbol(sym.BREAK, yytext());}
"class"		{ return new_symbol(sym.CLASS, yytext());}
"else"		{ return new_symbol(sym.ELSE, yytext());}
"const"		{ return new_symbol(sym.CONST, yytext());}
"if"		{ return new_symbol(sym.IF, yytext());}
"do" 		{ return new_symbol(sym.DO, yytext());}
"while"		{ return new_symbol(sym.WHILE, yytext());}
"new"		{ return new_symbol(sym.NEW, yytext());}
"print" 	{ return new_symbol(sym.PRINT, yytext()); }
"read"		{ return new_symbol(sym.READ, yytext());}
"return" 	{ return new_symbol(sym.RETURN, yytext()); }
"void" 		{ return new_symbol(sym.VOID, yytext()); }
"continue"	{ return new_symbol(sym.CONTINUE, yytext());}
"switch" 	{ return new_symbol(sym.SWITCH, yytext());}
"case" 		{ return new_symbol(sym.CASE, yytext());}

"+" 		{ return new_symbol(sym.PLUS, yytext()); }
"-"			{ return new_symbol(sym.MINUS, yytext());}
"*"			{ return new_symbol(sym.MUL, yytext());}
"/"			{ return new_symbol(sym.DIV, yytext());}
"%"			{ return new_symbol(sym.MOD, yytext());}

"=="		{ return new_symbol(sym.ISEQUAL, yytext());}
"!="		{ return new_symbol(sym.NOTEQUAL, yytext());}
">"			{ return new_symbol(sym.GREATER, yytext());}
">="		{ return new_symbol(sym.GREATEREQ, yytext());}
"<"			{ return new_symbol(sym.LESS, yytext());}
"<="		{ return new_symbol(sym.LESSEQ, yytext());}

"&&"		{ return new_symbol(sym.AND, yytext());}
"||"		{ return new_symbol(sym.OR, yytext());}

"=" 		{ return new_symbol(sym.EQUAL, yytext()); }
"++" 		{ return new_symbol(sym.INC, yytext());}
"--"		{ return new_symbol(sym.DEC, yytext());}

";" 		{ return new_symbol(sym.SEMI, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"."			{ return new_symbol(sym.DOT, yytext());}
"(" 		{ return new_symbol(sym.LPAREN, yytext()); }
")" 		{ return new_symbol(sym.RPAREN, yytext()); }
"{" 		{ return new_symbol(sym.LBRACE, yytext()); }
"}"			{ return new_symbol(sym.RBRACE, yytext()); }
"["			{ return new_symbol(sym.LSQBRACE, yytext());}
"]"			{ return new_symbol(sym.RSQBRACE, yytext());}

<YYINITIAL> "//" { yybegin(COMMENT); }
<COMMENT> .      { yybegin(COMMENT); }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

"true" | "false"		{ return new_symbol(sym.BOOL, new Boolean(yytext()));}
"'"."'"					{ return new_symbol(sym.CHAR, new Character(yytext().charAt(1))); }

[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }
([a-z]|[A-Z])[a-zA-Z0-9_]* 	{return new_symbol (sym.IDENT, yytext()); }

. { report_error("Leksicka greska ("+yytext()+") u liniji "+(yyline+1) + " na mestu " + yycolumn, yyline+1); }






