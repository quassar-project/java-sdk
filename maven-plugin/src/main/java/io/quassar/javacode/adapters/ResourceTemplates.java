package io.quassar.javacode.adapters;

import io.quassar.javacode.ports.SourceTemplates;

import io.quassar.templating.TemplateEngine;
import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.ports.TemplateReadException;
import io.quassar.templating.model.Template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public final class ResourceTemplates implements SourceTemplates {
	private final Map<String, Template> templates;

	public ResourceTemplates(Class<?> resources, List<String> names) {
		Map<String, Template> templates = new HashMap<>();
		for (String name : names) templates.put(name, load(resources, name));
		this.templates = Map.copyOf(templates);
	}

	@Override
	public String render(String name, Map<String, String> values) {
		ReflectiveFrameBuilder frame = new ReflectiveFrameBuilder(name.replace("-", ""));
		values.forEach(frame::add);
		return TemplateEngine.with(templates.get(name)).render(frame.toFrame());
	}

	private Template load(Class<?> resources, String name) {
		try (InputStream input = resources.getResourceAsStream("/templates/" + name + ".itr")) {
			return new TemplateReader(input).read();
		} catch (IOException | TemplateReadException exception) {
			throw new IllegalStateException("Could not load template " + name, exception);
		}
	}
}
