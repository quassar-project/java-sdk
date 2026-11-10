package example;

import io.quassar.templating.TemplateEngine;

public final class Consumer {
	public static String summary(String title) {
		return Library.summary(title);
	}

	public static Class<TemplateEngine> engineType() {
		return TemplateEngine.class;
	}
}
