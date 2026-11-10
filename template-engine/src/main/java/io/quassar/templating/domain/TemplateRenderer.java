package io.quassar.templating.domain;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.Trigger;
import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.Formatter;
import io.quassar.templating.ports.FrameFactory;

import io.quassar.templating.domain.formatters.StandardFormatters;
import io.quassar.templating.model.outputs.Output;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template.Configuration;
import io.quassar.templating.model.outputs.Expression;
import io.quassar.templating.model.outputs.Literal;
import io.quassar.templating.model.outputs.Placeholder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static io.quassar.templating.model.Trigger.resolve;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public final class TemplateRenderer {
	private static final String BLANKS = "  \t\n";
	private static final String NEW_LINE = "\n";
	private final Iterable<Rule> ruleSet;
	private final Configuration configuration;
	private final Map<String, Formatter> formatters;
	private final Map<Class<?>, ObjectMapper> mappers;

	private FrameFactory frameFactory;

	public TemplateRenderer(Iterable<Rule> ruleSet, Configuration configuration, FrameFactory frameFactory) {
		this.ruleSet = ruleSet;
		this.configuration = configuration;
		this.frameFactory = frameFactory;
		this.formatters = StandardFormatters.forLocale(configuration.locale());
		this.mappers = new HashMap<>();
	}

	public void using(FrameFactory factory) {
		this.frameFactory = factory;
	}

	public TemplateRenderer add(String name, Formatter formatter) {
		formatters.put(name.toLowerCase(), formatter);
		return this;
	}

	public TemplateRenderer addAll(Map<String, Formatter> formatters) {
		formatters.forEach(this::add);
		return this;
	}

	public <T> TemplateRenderer add(Class<T> type, ObjectMapper<T> mapper) {
		mappers.put(type, mapper);
		return this;
	}

	public String render(Object object) {
		Frame frame = frameOf(object);
		Canvas result = new Display(new Trigger("root").on(frame)).generate();
		return result.contentWith(configuration);
	}

	private Frame frameOf(Object object) {
		return object instanceof Frame frame ? frame : frameFactory.create(object, mappers);
	}

	private class Display {
		private static final Rule THIS_OUTPUT = new Rule().output(Placeholder.This);
		private static final Rule EMPTY_RULE = new Rule().output();
		private final Trigger trigger;
		private final Canvas canvas;

		Display(Trigger trigger) {
			this.trigger = trigger;
			this.canvas = new Canvas();
		}

		private Display set(boolean constant) {
			if (constant) canvas.touch();
			return this;
		}

		Canvas generate() {
			return generate(ruleFor(trigger).outputs());
		}

		private Canvas generate(Stream<Output> outputs) {
			outputs.forEach(this::write);
			return canvas;
		}

		private Rule ruleFor(Trigger trigger) {
			for (Rule rule : ruleSet) if (rule.condition().evaluate(trigger)) return rule;
			return defaultRule(trigger.frame());
		}

		private Rule defaultRule(Frame frame) {
			return hasValue(frame) ? THIS_OUTPUT : EMPTY_RULE;
		}

		private void write(Output output) {
			if (output instanceof Literal literal) write(literal);
			else if (output instanceof Placeholder placeholder) write(placeholder);
			else if (output instanceof Expression expression) write(expression);
		}

		private void write(Literal literal) {
			append(canvas, literal.toString());
		}

		private void write(Placeholder placeholder) {
			append(canvas, placeholder.isThis() ? evaluateThis(trigger.frame(), placeholder.formatters()) : evaluate(placeholder));
		}

		private void write(Expression expression) {
			Canvas result = evaluate(expression);
			if (result.isNotTouched() || result.isEmpty()) canvas.backSpaces();
			append(canvas, result);
		}

		private Canvas evaluateThis(Frame frame, String[] formatters) {
			return hasValue(frame) ? evaluateThis(format(frame, formatters)) : new Canvas();
		}

		private Frame format(Frame frame, String[] formatters) {
			if (formatters.length == 0) return frame;
			Object object = frame.value() != null ? frame.value() : frame;
			for (String name : formatters)
				object = formatter(name).format(object);
			return frameOf(object);
		}

		private Formatter formatter(String name) {
			name = name.trim().toLowerCase();
			return formatters.getOrDefault(name, Formatter.Null);
		}

		private Canvas evaluateThis(Frame frame) {
			return new Canvas().append(valueOf(frame)).touch();
		}

		private Canvas evaluate(Placeholder placeholder) {
			return hasContent(placeholder) ? evaluate(placeholder, trigger.frames(placeholder)) : new Canvas();
		}

		private Canvas evaluate(Placeholder placeholder, Iterator<Frame> frames) {
			Canvas canvas = new Canvas();
			while (frames.hasNext()) {
				Frame frame = frames.next();
				append(canvas, evaluate(placeholder, frame), placeholder.separator());
			}
			return canvas;
		}

		private Canvas evaluate(Placeholder placeholder, Frame frame) {
			Canvas canvas = new Display(new Trigger(placeholder.fullName()).on(format(frame, placeholder.formatters()))).generate();
			if (canvas.isNotEmpty()) canvas.touch();
			return canvas;
		}

		private boolean hasValue(Frame frame) {
			return frame.value() != null;
		}

		private boolean hasContent(Placeholder placeholder) {
			Frame frame = resolve(trigger.frame(), placeholder.targetPath());
			return frame.frames(placeholder.name()).hasNext();
		}

		private Canvas evaluate(Expression expression) {
			Canvas canvas = new Display(trigger)
					.set(expression.isConstant())
					.generate(expression.outputs());
			return canvas.isTouched() ? canvas.stake() : validate(expression.next());
		}

		private Canvas validate(Expression expression) {
			return expression != null ? evaluate(expression) : new Canvas();
		}

		private void append(Canvas canvas, String str) {
			canvas.append(str);
		}

		private void append(Canvas canvas, Canvas result, String separator) {
			if (result.isNotTouched()) return;
			if (canvas.isNotEmpty()) canvas.append(separator);
			canvas.append(result.toString()).touch();
		}

		private void append(Canvas canvas, Canvas result) {
			if (result.isNotTouched()) return;
			canvas.append(indent(result.toString())).touch();
		}

		private String valueOf(Frame frame) {
			return frame.value().toString();
		}

		private String indent(String str) {
			return canvas.hasNewLines() ? replaceNewLines(str) : str;
		}

		private String replaceNewLines(String str) {
			return str.replace(NEW_LINE, NEW_LINE + indentOf(canvas.toString()));
		}

		private String indentOf(String str) {
			return onlyBlanks(str.substring(lastNewLine(str) + 1));
		}

		private int lastNewLine(String str) {
			return str.lastIndexOf(NEW_LINE);
		}

		private String onlyBlanks(String str) {
			if (str.isEmpty()) return "";
			int first = str.charAt(0) == '\uFFFF' ? 1 : 0;
			int last = first;
			while ((last < str.length()) && isBlank(str.charAt(last))) last++;
			return str.substring(first, last);
		}
	}

	private static class Canvas {
		private final StringBuilder content;
		private boolean touched;

		public Canvas() {
			this.content = new StringBuilder();
			touched = false;
		}

		Canvas append(String s) {
			content.append(s);
			return this;
		}

		String contentWith(Configuration configuration) {
			return clean(withoutStakes(content)
					.toString())
					.collect(joining(configuration.isCRLF() ? "\r\n" : "\n"));
		}

		private Stream<String> clean(String str) {
			return clean(str.split(NEW_LINE));
		}

		private Stream<String> clean(String[] lines) {
			return stream(lines).map(this::cleanLine);
		}

		private String cleanLine(String line) {
			return line.replaceAll("^\\s*$", "");
		}

		Canvas touch() {
			this.touched = true;
			return this;
		}

		boolean isTouched() {
			return touched;
		}

		boolean isNotTouched() {
			return !isTouched();
		}

		Canvas stake() {
			content.appendCodePoint(0xFFFF);
			return this;
		}

		private StringBuilder withoutStakes(StringBuilder content) {
			return content.codePoints()
					.filter(c -> c != 0xFFFF)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
		}

		public boolean isEmpty() {
			return content.isEmpty();
		}

		public boolean isNotEmpty() {
			return !content.isEmpty();
		}

		private void backSpaces() {
			int index = content.length() - 1;
			while ((index >= 0) && isBlank(content.charAt(index))) index--;
			content.delete(index + 1, content.length());
		}

		public boolean hasNewLines() {
			return content.indexOf(NEW_LINE) >= 0;
		}

		@Override
		public String toString() {
			return content.toString();
		}
	}

	private static boolean isBlank(char c) {
		return BLANKS.indexOf(c) >= 0;
	}
}
