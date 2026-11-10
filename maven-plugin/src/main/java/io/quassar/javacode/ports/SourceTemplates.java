package io.quassar.javacode.ports;

import java.util.Map;

@FunctionalInterface
public interface SourceTemplates {
	String render(String name, Map<String, String> values);
}
