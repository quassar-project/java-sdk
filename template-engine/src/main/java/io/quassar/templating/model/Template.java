package io.quassar.templating.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.quassar.templating.model.Template.Configuration.LineSeparator.CRLF;
import static io.quassar.templating.model.Template.Configuration.LineSeparator.LF;

public class Template {
	public static final Template EMPTY = new Template(new Configuration(), List.of());

	private final Configuration configuration;
	private final List<Rule> rules;

	public Template() {
		this(new Configuration());
	}

	public Template(Configuration configuration) {
		this(configuration, new ArrayList<>());
	}

	private Template(Configuration configuration, List<Rule> rules) {
		this.configuration = configuration;
		this.rules = rules;
	}

	public Template add(Rule rule) {
		rules.add(rule);
		return this;
	}

	public List<Rule> ruleSet() {
		return List.copyOf(rules);
	}

	public Configuration configuration() {
		return configuration;
	}

	public static Template compose(Template first, Template... templates) {
		Template result = new Template();
		first.ruleSet().forEach(result::add);
		for (Template template : templates) template.ruleSet().forEach(result::add);
		return result;
	}

	protected Rule rule() {
		return new Rule();
	}

	public record Configuration(Locale locale, LineSeparator separator) {
		public Configuration() {
			this(Locale.ENGLISH, LF);
		}

		public boolean isCRLF() {
			return separator() == CRLF;
		}

		public enum LineSeparator {
			LF, CRLF
		}
	}

}
