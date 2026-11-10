package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Trigger;

import static io.quassar.templating.model.predicates.BinaryOperator.AND;

public record BinaryExpression(LogicalExpression left, BinaryOperator operator,
							   LogicalExpression right) implements LogicalExpression {
	@Override
	public boolean evaluate(Trigger trigger) {
		if (operator == AND) return left.evaluate(trigger) && right.evaluate(trigger);
		return left.evaluate(trigger) || right.evaluate(trigger);
	}

	@Override
	public String toString() {
		return embrace(left) + " " + operator.name() + " " + embrace(right);
	}

	private String embrace(LogicalExpression expression) {
		String result = expression.toString();
		return expression instanceof Predicate ? result : "(" + result + ")";
	}
}
