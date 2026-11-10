package io.quassar.templating.domain.verification;

import io.quassar.templating.domain.formatters.StandardFormatters;
import io.quassar.templating.model.Template;

import java.util.Set;
import java.util.function.Predicate;

public final class CompletenessVerifier extends FrameVerifier {
	public CompletenessVerifier(Template template) {
		super(template, supportedFormatters(template));
	}

	private static Predicate<String> supportedFormatters(Template template) {
		Set<String> names = Set.copyOf(StandardFormatters.forLocale(template.configuration().locale()).keySet());
		return names::contains;
	}
}
