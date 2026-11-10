package io.quassar.templating.domain.formatters;

import io.quassar.templating.ports.Formatter;
import io.quassar.templating.domain.formatters.spelling.EnglishNumberSpelling;
import io.quassar.templating.domain.formatters.spelling.SpanishNumberSpelling;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumberFormatters {

	public static Map<String, Formatter> get(Locale locale) {
		Map<String, Formatter> map = map();
		map.put("Words", numberSpellingFormatter(locale));
		map.put("Separators", separators(locale));
		map.put("TwoDecimals", twoDecimals(locale));
		return map;
	}

	private static HashMap<String, Formatter> map() {
		return new HashMap<String, Formatter>() {
			@Override
			public Formatter put(String key, Formatter value) {
				return super.put(key.toLowerCase(), value);
			}
		};
	}

	private static Formatter separators(final Locale locale) {
		return value -> {
			if (!isNumber(value)) return value;
			DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
			df.setGroupingUsed(true);
			df.setGroupingSize(3);
			return df.format(value);
		};
	}

	private static Formatter twoDecimals(final Locale locale) {
		return value -> {
			if (!isNumber(value)) return value;
			double n = ((Number) value).doubleValue();
			return String.format(locale, "%.2f", n);
		};
	}

	private static Formatter numberSpellingFormatter(Locale locale) {
		NumberSpelling spelling = (locale.getLanguage().equals("es")) ?
				new SpanishNumberSpelling() :
				new EnglishNumberSpelling();
		return value -> isNumber(value) ? spelling.spell((Integer) value) : value;
	}

	private static boolean isNumber(Object value) {
		return Number.class.isAssignableFrom(value.getClass());
	}

	public interface NumberSpelling {
		String spell(int number);
	}

}
