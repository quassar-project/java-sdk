lexer grammar ItrLexer;

@lexer::members {
	int lastMode = 0;
	boolean inBody = false;
	private void setLastMode(int i) {
		lastMode = i;
	}

	public int getLastMode() {
        return lastMode;
    }

    public boolean markHasParameters() {
        if (getCharIndex() == this.getInputStream().toString().length()) return false;
        char c = this.getInputStream().toString().charAt(getCharIndex());
        String next = "";
        if (getCharIndex() + 3 < getInputStream().toString().length())
            next = this.getInputStream().toString().substring(getCharIndex(), getCharIndex() + 3);
        return (c == '+' || next.equals("..."));
    }

    public void setMode(int newMode) {
        _mode = newMode;
    }

    public void exitMark() {
        if(!markHasParameters()) {
			setMode(lastMode);
			setLastMode(MARK_MODE);
        }
    }
}


BEGIN_RULE             : 'rule'                                  { setMode(SIGNATURE_MODE); setLastMode(DEFAULT_MODE);};
WL                     : (' '|'\t')* ('\r'? '\n' | '\n')        -> skip;
BEGIN_BODY             : 'body'                          		-> skip;
COMMENT                : .;

mode SIGNATURE_MODE;
	NOT                : 'not';
	AND                : 'and';
	OR                 : 'or';
	NAME      		   : LETTER(DIGIT|LETTER|SCORE)*;
	END_SIGNATURE      : (' '|'\t')* NL ('\t' | '    ')?        { setLastMode(SIGNATURE_MODE); setType(BEGIN_BODY);} -> mode(BODY_MODE);
	WS                 : SP+                                    -> skip;
	LPAREN			   : '(';
	RPAREN			   : ')';
	COMMA			   : ',';
	OTHER              : .;

mode BODY_MODE;
	END_RULE_SCAPPED   : '~rule'                                { setText("rule"); setType(TEXT);};
	END_RULE           : NL NL? 'rule'                          { setType(BEGIN_RULE);setMode(SIGNATURE_MODE); setLastMode(BODY_MODE);};
	NEWLINE            : NL ('\t' | '    ')?                    { setText("\n"); setType(TEXT);};
	CURL               : '$~'                                   { setText("~"); setType(TEXT);};
	DOLLAR             : '$$'                                   { setText("$"); setType(TEXT);};
	LSB                : '$<<'                                  { setText("<<"); setType(TEXT);};
	RSB                : '$>>'                                  { setText(">>"); setType(TEXT);};
	EXPRESSION_LT      : '<'                                    { setText("<"); setType(TEXT);};
	TRIGGER            : '$'                                    { setMode(MARK_MODE); setLastMode(BODY_MODE);};
	BEGIN_EXPRESSION   : '<<'                                   { setType(BEGIN_EXPRESSION);setMode(EXPRESSION_MODE); setLastMode(BODY_MODE);};
	NULL_SEPARATOR     : '~'                                    -> skip;
	TEXT               : ~('$' | '\r' | '\n' | '~'| '<')+;

mode MARK_MODE;
	LIST               : '...';
	TARGET             : '<' (~'>')* '>';
	OPTION             : '+'                                    { setType(OPTION);};
    NULL               : '~'                                    { setMode(lastMode); setLastMode(MARK_MODE);}-> skip;
	SEPARATOR          : '[' (~']')* ']'                        { setMode(lastMode); setLastMode(MARK_MODE);};
	ID                 : (LETTER) (DIGIT|LETTER | '_' | '-')* (DIGIT|LETTER)       { setType(ID); exitMark();};
	MARK_ERROR         : .;

mode EXPRESSION_MODE;
	ELSE			   : '?';
	END_EXPRESSION     : '>>'                                   { setType(END_EXPRESSION);setLastMode(EXPRESSION_MODE);} -> mode(BODY_MODE);
	EXPRESSION_QUESTION: '$?'                                   { setText("?"); setType(TEXT);};
	EXPRESSION_DOLLAR  : '$$'                                   { setText("$"); setType(TEXT);};
    EXPRESSION_LSB     : '$<<'                                  { setText("<<"); setType(TEXT);};
    EXPRESSION_RSB     : '$>>'                                  { setText(">>"); setType(TEXT);};
    EXPRESSION_GT      : '>'                                    { setText(">"); setType(TEXT);};
	EXPRESSION_NULL    : '~'                                    -> skip;
	EXPRESSION_TRIGGER : '$'                                    { setType(TRIGGER); setLastMode(EXPRESSION_MODE);} -> mode(MARK_MODE);
	EXPRESSION_TEXT    : ~('?' |'$'| '\n' | '>')+         		{ setType(TEXT);};
	EXPRESSION_NL      : NL ('\t' | '    ')?                    { setText("\n"); setType(TEXT);};
	EXPRESSION_ERROR   : .;

fragment
	NL              : ('\r'? '\n' | '\n');
fragment
	SP              : (' ' | '\t');
fragment
	DIGIT           :[0-9];
fragment
	LETTER          : 'a'..'z' | 'A'..'Z';
fragment
	SCORE           : '-' | '_';
