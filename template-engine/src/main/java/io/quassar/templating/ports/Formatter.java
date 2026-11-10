package io.quassar.templating.ports;

public interface Formatter {
	Formatter Null = value -> value;

	Object format(Object value);
}
