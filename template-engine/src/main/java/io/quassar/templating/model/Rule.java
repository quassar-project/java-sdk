package io.quassar.templating.model;

import io.quassar.templating.model.predicates.LogicalExpression;
import io.quassar.templating.model.outputs.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Rule {
	private LogicalExpression condition;
	private final List<Output> outputs = new ArrayList<>();

	public Rule condition(LogicalExpression expression) {
		this.condition = expression;
		return this;
	}

	public Rule output(Output... outputs) {
		this.outputs.addAll(asList(outputs));
		return this;
	}

	public LogicalExpression condition() {
		return condition;
	}

	public Stream<Output> outputs() {
		return outputs.stream();
	}

}
