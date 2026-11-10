package io.quassar.templating.domain.formatters;

import io.quassar.templating.ports.Formatter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateFormatters {

	public static Map<String, Formatter> get(Locale locale) {
		Map<String, Formatter> map = map();
		map.put("Year", year(locale));
		map.put("YearMonth", yearMonth(locale));
		map.put("YearMonthDay", yearMonthDay(locale));
		map.put("YearMonthDayTime", yearMonthDayTime(locale));
		map.put("ShortDate", shortDate(locale));
		map.put("FullDate", fullDate(locale));
		map.put("CompactDate", compactDate(locale));
		map.put("AmericanCompactDate", americanCompactDate(locale));
		map.put("AmericanDate", americanDate(locale));
		map.put("DayOfWeek", dayOfWeek(locale));
		map.put("Time", time(locale));
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

	private static Formatter year(Locale locale) {
		return value -> format(value, "yyyy", locale);
	}

	private static Formatter yearMonth(Locale locale) {
		return value -> format(value, "yyyyMM", locale);
	}

	private static Formatter yearMonthDay(Locale locale) {
		return value -> format(value, "yyyyMMdd", locale);
	}

	private static Formatter yearMonthDayTime(Locale locale) {
		return value -> format(value, "yyyyMMddhhmmss", locale);
	}

	private static Formatter shortDate(Locale locale) {
		return value -> format(value, "dd/MM/yyyy", locale);
	}

	private static Formatter americanCompactDate(Locale locale) {
		return value -> format(value, "MMM, dd yyyy", locale);
	}

	private static Formatter americanDate(Locale locale) {
		return value -> format(value, "MMMM, dd yyyy", locale);
	}

	private static Formatter fullDate(Locale locale) {
		return value -> format(value, "EEEE, dd MMMM yyyy", locale);
	}

	private static Formatter compactDate(Locale locale) {
		return value -> format(value, "EEE, dd MMM yyyy", locale);
	}

	private static Formatter dayOfWeek(Locale locale) {
		return value -> format(value, "EEEE", locale);
	}

	private static Formatter time(Locale locale) {
		return value -> format(value, "HH:mm", locale);
	}

	private static Object format(Object value, String pattern, Locale locale) {
		return value instanceof Temporal ? format((Temporal) value, pattern, locale) : value;
	}

	private static String format(Temporal value, String pattern, Locale locale) {
		return DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(zoneOf(value)).format(value);
	}

	private static ZoneId zoneOf(Temporal value) {
		return value instanceof ZonedDateTime ? ((ZonedDateTime) value).getZone() : ZoneId.systemDefault();
	}
}
