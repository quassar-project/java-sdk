package io.quassar.templating.model.outputs;

public class Outputs {
	public static Placeholder placeholder(String[] context, String name, String... formatters) {
		return new Placeholder(name, context, formatters);
	}

	public static Placeholder placeholder(String name, String... formatters) {
		return new Placeholder(name, formatters);
	}

	public static Literal literal(String text) {
		return new Literal(text);
	}

	public static Expression expression(Output... outputs) {
		return new Expression(outputs);
	}
}
