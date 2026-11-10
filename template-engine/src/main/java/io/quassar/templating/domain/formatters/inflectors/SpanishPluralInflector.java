package io.quassar.templating.domain.formatters.inflectors;

import io.quassar.templating.domain.formatters.StringFormatters;

public class SpanishPluralInflector extends StringFormatters.PluralInflector {

	@Override
	public String plural(String word) {
		if (isIrregular(word)) return irregularPlural(word);
		return doReplaces(word + "es");
	}

	@Override
	protected void setReplaces() {
		addReplace("zes", "ces");
		addReplace("xes", "x");
		addReplace("ses", "s");
		addReplace("aes", "as");
		addReplace("ees", "es");
		addReplace("ies", "is");
		addReplace("oes", "os");
		addReplace("ues", "us");
	}

	@Override
	protected void setIrregulars() {

	}

}
