package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Trigger;

public record NotExpression(LogicalExpression expression) implements Predicate {

	@Override
	public boolean evaluate(Trigger trigger) {
		return !expression.evaluate(trigger);
	}

	@Override
	public String toString() {
		return "NOT " + expression.toString();
	}
}
