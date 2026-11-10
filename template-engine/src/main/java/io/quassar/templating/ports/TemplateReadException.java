package io.quassar.templating.ports;

public class TemplateReadException extends Exception {
	public TemplateReadException(String message) {
		super(message);
	}
	public TemplateReadException(String message, Throwable cause) {
		super(message, cause);
	}
}
