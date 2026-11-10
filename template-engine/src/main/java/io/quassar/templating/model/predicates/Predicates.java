package io.quassar.templating.model.predicates;

import static io.quassar.templating.model.predicates.BinaryOperator.AND;
import static io.quassar.templating.model.predicates.BinaryOperator.OR;

public class Predicates {
	public static Predicate not(LogicalExpression expression) {
		return new NotExpression(expression);
	}

	public static Predicate attribute(String attribute) {
		return new AttributePredicate(attribute);
	}

	public static Predicate attribute(String attribute, Object value) {
		return new AttributePredicate(attribute, value);
	}

	public static Predicate type(String type) {
		return new TypePredicate(type);
	}

	public static Predicate allTypes(String... types) {
		return new TypePredicate(types);
	}

	public static Predicate trigger(String name) {
		return new TriggerPredicate(name);
	}

	public static BinaryExpression all(LogicalExpression left, LogicalExpression right) {
		return new BinaryExpression(left, AND, right);
	}

	public static LogicalExpression all(LogicalExpression... expressions) {
		LogicalExpression root = expressions[0];
		if (expressions.length == 1) return root;
		for (int i = 0; i < expressions.length - 1; i++)
			root = new BinaryExpression(root, AND, expressions[i + 1]);
		return root;
	}

	public static BinaryExpression any(LogicalExpression left, LogicalExpression right) {
		return new BinaryExpression(left, OR, right);
	}

	public static LogicalExpression any(LogicalExpression... expressions) {
		LogicalExpression root = expressions[0];
		if (expressions.length == 1) return root;
		for (int i = 0; i < expressions.length - 1; i++)
			root = new BinaryExpression(root, OR, expressions[i + 1]);
		return root;
	}
}
