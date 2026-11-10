package io.quassar.templating.adapters.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExcludingReflectionMapper<T> extends ReflectionMapper<T> {
	private final List<String> fields = new ArrayList<>();

	public ExcludingReflectionMapper(String... fields) {
		Collections.addAll(this.fields, fields);
	}

	@Override
	protected boolean isProcessable(Field field) {
		return super.isProcessable(field) && !this.fields.contains(field.getName());
	}
}
