package io.quassar.templating.ports;

import io.quassar.templating.model.Template;

@FunctionalInterface
public interface TemplateLoader {
	Template read() throws TemplateReadException;
}
