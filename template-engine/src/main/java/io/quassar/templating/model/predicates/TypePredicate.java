package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Trigger;

import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class TypePredicate implements Predicate {
	private final Checker checker;
	private final String[] types;

	public TypePredicate(String... types) {
		this.types = types;
		this.checker = types.length > 1 ?
				multiple(setOf(types)) :
				single(types[0].toLowerCase());
	}

	@Override
	public boolean evaluate(Trigger trigger) {
		return checker.check(trigger);
	}

	@SuppressWarnings("unused")
	public String[] types() {
		return types;
	}

	private Checker multiple(Set<String> types) {
		return trigger -> types.stream().allMatch(t -> trigger.frame().is(t));
	}

	private Checker single(String type) {
		return trigger -> trigger.frame().is(type);
	}

	private Set<String> setOf(String[] types) {
		return stream(types).map(String::toLowerCase).collect(toSet());
	}

	@Override
	public String toString() {
		return "type(" + String.join(", ", types) + ")";
	}

	interface Checker {
		boolean check(Trigger trigger);
	}
}
