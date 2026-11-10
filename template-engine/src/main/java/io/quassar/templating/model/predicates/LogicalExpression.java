package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Trigger;

public interface LogicalExpression {
	boolean evaluate(Trigger trigger);
}
