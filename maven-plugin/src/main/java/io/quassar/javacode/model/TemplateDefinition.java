package io.quassar.javacode.model;

import io.quassar.templating.model.Rule;

import java.util.List;

public record TemplateDefinition(String filename, List<Rule> rules) {
	public TemplateDefinition {
		rules = List.copyOf(rules);
	}
}
