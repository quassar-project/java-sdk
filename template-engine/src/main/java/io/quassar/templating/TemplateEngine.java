package io.quassar.templating;

import io.quassar.templating.adapters.reflection.ObjectFrameFactory;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.ports.TemplateReadException;
import io.quassar.templating.domain.TemplateRenderer;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;
import io.quassar.templating.model.Template.Configuration;
import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.Formatter;
import io.quassar.templating.ports.FrameFactory;

import io.quassar.templating.ports.TemplateLoader;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public final class TemplateEngine {
	private final TemplateRenderer renderer;

	private TemplateEngine(Iterable<Rule> rules, Configuration configuration, FrameFactory frames) {
		renderer = new TemplateRenderer(rules, configuration, frames);
	}

	public static TemplateEngine with(Template template) {
		return with(template.ruleSet(), template.configuration());
	}

	public static TemplateEngine with(Iterable<Rule> rules) {
		return with(rules, new Configuration());
	}

	public static TemplateEngine with(Iterable<Rule> rules, Configuration configuration) {
		return new TemplateEngine(rules, configuration, new ObjectFrameFactory());
	}

	public static TemplateEngine with(InputStream input) throws TemplateReadException {
		return with(new TemplateReader(input));
	}

	public static TemplateEngine with(Path path) throws TemplateReadException {
		return with(TemplateReader.read(path));
	}

	public static TemplateEngine with(TemplateLoader loader) throws TemplateReadException {
		return with(loader.read());
	}

	public TemplateEngine using(FrameFactory factory) {
		renderer.using(factory);
		return this;
	}

	public TemplateEngine add(String name, Formatter formatter) {
		renderer.add(name, formatter);
		return this;
	}

	public TemplateEngine addAll(Map<String, Formatter> formatters) {
		renderer.addAll(formatters);
		return this;
	}

	public <T> TemplateEngine add(Class<T> type, ObjectMapper<T> mapper) {
		renderer.add(type, mapper);
		return this;
	}

	public String render(Object object) {
		return renderer.render(object);
	}
}
