package io.quassar.javacode.model;

import io.quassar.templating.model.Rule;

import java.util.List;

public record TemplateDefinition(String filename, List<Rule> rules) {
	public TemplateDefinition {
		rules = List.copyOf(rules);
	}
	public String name() {
		String path = filename.replace('\\', '/');
		String local = path.substring(path.lastIndexOf('/') + 1);
		return local.substring(0, local.length() - ".itr".length());
	}
}
