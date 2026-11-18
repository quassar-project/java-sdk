package io.quassar.javacode.model;

import java.util.List;
import java.util.Optional;

public record StarterDefinition(String packageName, Optional<String> modelName, String modelClass, List<JavaCode> templates) {
	public StarterDefinition {
		templates = List.copyOf(templates);
	}
}
