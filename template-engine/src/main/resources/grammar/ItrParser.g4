parser grammar ItrParser;
options { tokenVocab=ItrLexer; }

root            : COMMENT* (rule COMMENT*)+ EOF;
rule      		: BEGIN_RULE signature body;

signature       : condition BEGIN_BODY;
condition		: term (operator term)*;
operator		: AND | OR;
term			: NOT? (predicate | LPAREN condition RPAREN);
predicate		: NAME parameters;
parameters 		: LPAREN parameter (COMMA parameter)* RPAREN;
parameter		: (NAME | OTHER)+;

body            : (text | placeholder | expression)*;
expression      : BEGIN_EXPRESSION expressionBody+ (ELSE expressionBody+)* END_EXPRESSION;
expressionBody  : text | placeholder | expression;
text            : TEXT;
placeholder     : TRIGGER TARGET? ID option* (LIST SEPARATOR)?;
option          : OPTION ID;