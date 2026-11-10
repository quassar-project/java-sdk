package tests;

import io.quassar.templating.adapters.antlr.generated.ItrLexer;

import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.antlr.v4.runtime.CharStreams.fromString;
import static org.assertj.core.api.Assertions.assertThat;

public class LexerTest {

	@Test
	public void ruleBeginTest() {
		String expectedType = "BEGIN_RULE";
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/rules/rule-keyword"));
		assertThat(receivedTypes[0]).isEqualTo(expectedType);
	}

	@Test
	public void ruleSignatureTest() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/conditions/type-signature"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void textTest() {
		String[] receivedTypes = lexerTest("'Name'");
		assertThat(receivedTypes).hasSize(6);
	}

	@Test
	public void mark() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TRIGGER", "ID", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/slots/unicode-slot"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void markWithTarget() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TRIGGER", "TARGET", "ID", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/context/root-target"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void escapedCharacter() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT",
				"TEXT", "TEXT", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/literals/escaped-characters"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void expressionWithNewLines() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TEXT", "BEGIN_EXPRESSION", "TRIGGER", "ID",
				"TEXT", "TEXT", "TEXT", "TEXT", "END_EXPRESSION", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/expressions/multiline"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void noParametersFunction() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "RPAREN", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT",
				"TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/conditions/missing-parameters"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void OtherWithMark() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT",
				"TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/slots/adjacent-text"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void MarkWithCurlSeparator() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TRIGGER", "ID", "OPTION", "ID", "TEXT", "TEXT", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/literals/placeholder-suffix"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void doubleCurlSeparator() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/literals/escaped-dollar"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void MarkWithFormat() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "OTHER", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "TEXT",
				"TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/slots/formatter-chain"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void classWithMarks() {
		String[] expectedTypes = new String[]{"BEGIN_RULE",
				"NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "OTHER", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(
				TestTemplates.text("syntax/rules/combined-conditions")
		);
		assertThat(receivedTypes).containsExactly(expectedTypes);

	}

	@Test
	public void markWithModifiers() {
		String[] expectedTypes = new String[]{"BEGIN_RULE",
				"NAME", "LPAREN", "NAME", "RPAREN",
				"BEGIN_BODY", "TEXT",
				"TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT", "TRIGGER", "ID", "TEXT", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/lists/formatted-values"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void mediumTest() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT",
				"BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/expressions/optional-modifier"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void twoRules() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT",
				"BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT",
				"BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/rules/ordered-rules"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void ruleWithEval() {
		String[] expectedTypes = new String[]{"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "OTHER", "OTHER", "OTHER", "NAME", "OTHER", "RPAREN", "BEGIN_BODY", "TEXT",
				"BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY", "TEXT",
				"BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/conditions/custom-predicate"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void ruleWithListMarkInExpression() {
		String[] expectedTypes = new String[]{"BEGIN_RULE",
				"NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TRIGGER", "ID", "TEXT", "BEGIN_EXPRESSION", "TEXT", "TRIGGER", "ID", "LIST", "SEPARATOR", "END_EXPRESSION", "TEXT",
				"BEGIN_EXPRESSION", "TEXT", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "TEXT",
				"TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/generation/rule-builder"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void littleBigTest() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "BEGIN_EXPRESSION", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "TRIGGER", "ID", "TEXT", "BEGIN_EXPRESSION", "TEXT", "TRIGGER", "ID", "TEXT", "END_EXPRESSION", "BEGIN_EXPRESSION", "TEXT", "TRIGGER", "ID", "LIST", "SEPARATOR", "END_EXPRESSION", "TEXT", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT",
				"TEXT", "TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT", "TEXT",
				"TEXT",
				"TEXT"};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/generation/java-class"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void xmlTaraTest() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT",
				"TEXT", "TEXT", "TRIGGER", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT", "TEXT",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "TEXT", "TEXT", "TEXT",
				"TRIGGER", "ID", "LIST", "SEPARATOR", "TEXT", "TEXT",
				"TEXT",
				"TEXT",
		};
		String[] receivedTypes = lexerTest(
				TestTemplates.text("syntax/generation/xml-tree"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void xmlSmallTest() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "TEXT", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "AND",
				"NAME", "LPAREN", "NAME", "OTHER", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT",
				"TEXT"
		};
		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/generation/xml-interfaces"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	@Test
	public void xmlCompleteTest() {
		String[] expectedTypes = new String[]{
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT", "TRIGGER", "ID", "TEXT", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "TEXT",
				"TEXT", "TRIGGER", "ID", "OPTION", "ID", "LIST", "SEPARATOR", "TEXT",
				"TEXT",
				"TEXT",
				"TEXT",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "OTHER", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT",
				"BEGIN_RULE", "NAME", "LPAREN", "NAME", "OTHER", "NAME", "RPAREN", "BEGIN_BODY",
				"TEXT", "TEXT", "TRIGGER", "ID", "TEXT",
				"TEXT"};

		String[] receivedTypes = lexerTest(TestTemplates.text("syntax/generation/xml-optional-members"));
		assertThat(receivedTypes).containsExactly(expectedTypes);
	}

	private static String[] lexerTest(String query) {
		ArrayList<String> receivedTypes = new ArrayList<>();
		ItrLexer lexer = new ItrLexer(fromString(query));
		lexer.reset();
		String[] ruleNames = lexer.getRuleNames();
		for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken())
			receivedTypes.add(ruleNames[token.getType() - 1]);
		return receivedTypes.toArray(new String[0]);
	}
}
