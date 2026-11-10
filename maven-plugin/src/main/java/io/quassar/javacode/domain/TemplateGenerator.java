package io.quassar.javacode.domain;

import io.quassar.javacode.model.JavaCode;
import io.quassar.javacode.model.TemplateDefinition;
import io.quassar.javacode.ports.SourceTemplates;
import io.quassar.templating.model.Rule;
import io.quassar.templating.ports.TemplateReadException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TemplateGenerator {
	private final SourceTemplates templates;

	public TemplateGenerator(SourceTemplates templates) {
		this.templates = templates;
	}

	public List<JavaCode> generate(String packageName, List<TemplateDefinition> definitions) throws TemplateReadException {
		String templatePackage = packageName.isEmpty() ? "templates" : packageName + ".templates";
		JavaNames names = new JavaNames(Set.of());
		List<JavaCode> code = new ArrayList<>();
		for (TemplateDefinition definition : definitions) {
			String filename = Path.of(definition.filename()).getFileName().toString();
			String base = JavaNames.className(filename.substring(0, filename.length() - ".itr".length())) + "Template";
			String name = names.claim(base);
			code.add(new JavaCode(templatePackage, name, compile(templatePackage, name, definition)));
		}
		return List.copyOf(code);
	}

	private String compile(String packageName, String name, TemplateDefinition definition) throws TemplateReadException {
		List<Rule> rules = definition.rules();
		if (rules.isEmpty()) throw new TemplateReadException(definition.filename() + ": Template has no rules");
		try {
			TemplateRules java = new TemplateRules();
			StringBuilder additions = new StringBuilder();
			StringBuilder methods = new StringBuilder();
			for (int index = 0; index < rules.size(); index++) {
				String method = "rule" + (index + 1);
				additions.append("\n\t\t.add(").append(method).append("())");
				methods.append("\n\tprivate static Rule ").append(method).append("() {\n\t\treturn ")
					.append(java.rule(rules.get(index))).append(";\n\t}\n");
			}
			return templates.render("template-class", Map.of("package", packageName, "name", name,
				"rules", additions.toString(), "methods", methods.toString()));
		} catch (IllegalArgumentException exception) {
			throw new TemplateReadException(definition.filename() + ": " + exception.getMessage(), exception);
		}
	}
}
