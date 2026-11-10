package tests;

import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;
import io.quassar.templating.model.predicates.BinaryExpression;
import io.quassar.templating.model.predicates.LogicalExpression;
import io.quassar.templating.model.predicates.AttributePredicate;
import io.quassar.templating.model.predicates.TypePredicate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

	@Test
	public void test1() {
		Template template = TestTemplates.read("syntax/slots/adjacent-text");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test2() {
		Template template = TestTemplates.read("syntax/rules/combined-conditions");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test3() {
		Template template = TestTemplates.read("syntax/literals/escaped-characters");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test4() {
		Template template = TestTemplates.read("syntax/conditions/type-signature");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test5() {
		Template template = TestTemplates.read("syntax/rules/ordered-rules");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test6() {
		Template template = TestTemplates.read("syntax/generation/xml-tree");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test7() {
		Template template = TestTemplates.read("syntax/generation/java-class");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test8() {
		Template template = TestTemplates.read("syntax/generation/xml-optional-members");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test9() {
		Template template = TestTemplates.read("syntax/slots/unicode-slot");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test10() {
		Template template = TestTemplates.read("syntax/slots/formatter-chain");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test11() {
		Template template = TestTemplates.read("syntax/expressions/multiline");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test12() {
		Template template = TestTemplates.read("syntax/expressions/fallback-chain");
		assertThat(template.ruleSet()).isNotNull();
	}

	@Test
	public void test13() {
		Template template = TestTemplates.read("syntax/conditions/attribute-value");
		List<Rule> ruleset = template.ruleSet();
		assertThat(ruleset).hasSize(1);
		LogicalExpression condition = ruleset.get(0).condition();
		assertThat(condition).isInstanceOf(BinaryExpression.class);
		BinaryExpression expression = (BinaryExpression) condition;
		assertThat(expression.right()).isInstanceOf(AttributePredicate.class);
		assertThat(((AttributePredicate) expression.right()).attribute()).isEqualTo("a");
		assertThat(((AttributePredicate) expression.right()).value()).isEqualTo("b");
	}

	@Test
	public void test14() {
		Template template = TestTemplates.read("syntax/conditions/attribute-present");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).hasSize(1);
		LogicalExpression condition = rules.get(0).condition();
		assertThat(condition).isInstanceOf(BinaryExpression.class);
		BinaryExpression expression = (BinaryExpression) condition;
		assertThat(expression.right()).isInstanceOf(AttributePredicate.class);
		assertThat(((AttributePredicate) expression.right()).attribute()).isEqualTo("a");
		assertThat(((AttributePredicate) expression.right()).value()).isNull();
	}

	@Test
	public void test15() {
		Template template = TestTemplates.read("syntax/generation/conditional-java-members");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).isNotNull();
	}

	@Test
	public void test16() {
		Template template = TestTemplates.read("syntax/context/root-target");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).isNotNull();
	}

	@Test
	public void test17() {
		Template template = TestTemplates.read("syntax/conditions/formatter-predicate");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).isNotNull();
		LogicalExpression condition = rules.get(0).condition();
		assertThat(condition).isNotNull();
	}

	@Test
	public void test18() {
		Template template = TestTemplates.read("syntax/conditions/compound-predicate");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).isNotNull();
		LogicalExpression condition = rules.get(0).condition();
		assertThat(condition).isNotNull();
	}

	@Test
	public void testFull() {
		Template template = TestTemplates.read("syntax/generation/service-api");
		List<Rule> rules = template.ruleSet();
		assertThat(rules).isNotNull();
		LogicalExpression condition = rules.get(0).condition();
		assertThat(condition).isInstanceOf(TypePredicate.class);
		assertThat(((TypePredicate) condition).types()).isEqualTo(new String[]{"api", "service"});
	}

}
