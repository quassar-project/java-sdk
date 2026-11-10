package io.quassar.templating.domain.formatters;

import io.quassar.templating.ports.Formatter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class StandardFormatters {
	private StandardFormatters() {
	}

	public static Map<String, Formatter> forLocale(Locale locale) {
		Map<String, Formatter> formatters = new HashMap<>();
		formatters.putAll(StringFormatters.get(locale));
		formatters.putAll(NumberFormatters.get(locale));
		formatters.putAll(DateFormatters.get(locale));
		return formatters;
	}
}
