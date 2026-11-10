package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Trigger;

import java.util.stream.Stream;

import static java.util.Arrays.stream;

public record TriggerPredicate(String name) implements Predicate {
	public TriggerPredicate(String name) {
		this.name = name.toLowerCase();
	}

	@Override
	public boolean evaluate(Trigger trigger) {
		return trigger.name().equals(name) || formattersIn(trigger.name()).anyMatch(s -> s.equals(name));
	}

	private Stream<String> formattersIn(String trigger) {
		return stream(skipName(trigger).split("\\+"));
	}

	private String skipName(String trigger) {
		return trigger.contains("+") ? trigger.substring(trigger.indexOf('+')) : "";
	}

	@Override
	public String toString() {
		return "trigger(" + name + ")";
	}
}
