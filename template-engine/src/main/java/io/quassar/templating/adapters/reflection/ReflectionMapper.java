package io.quassar.templating.adapters.reflection;

import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.FrameBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;
import static java.lang.reflect.Modifier.isTransient;

public class ReflectionMapper<T> implements ObjectMapper<T> {

	@Override
	public void map(T source, FrameBuilder context) {
		new SlotBuilder(source, context).create();
	}

	private boolean isExcluded(Field field) {
		return isStatic(field.getModifiers()) ||
				isTransient(field.getModifiers()) ||
				isContextFieldOfInnerClass(field);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	protected boolean isProcessable(Field field) {
		return !isExcluded(field);
	}

	private boolean isContextFieldOfInnerClass(Field field) {
		return field.getName().startsWith("this$");
	}

	private static class Item {
		private final Object key;
		private final Object value;

		Item(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	private class SlotBuilder {
		private static final String Count = "Count";
		private final FrameBuilder context;
		private final Object source;

		SlotBuilder(Object source, FrameBuilder context) {
			this.context = context;
			this.source = source;
		}

		void create() {
			try {
				execute(source.getClass());
			} catch (IllegalAccessException exception) {
				throw new IllegalStateException("Could not adapt " + source.getClass().getName(), exception);
			}
		}

		private void execute(Class aClass) throws IllegalAccessException {
			while (aClass != null) {
				processClass(aClass);
				aClass = aClass.getSuperclass();
			}
		}

		private void processClass(Class aClass) throws IllegalAccessException {
			for (Field field : aClass.getDeclaredFields()) {
				if (!isProcessable(field)) continue;
				processField(field);
			}
		}

		private void processField(Field field) throws IllegalAccessException {
			boolean accessibility = field.isAccessible();
			field.setAccessible(true);
			if (field.get(source) != null) processField(field.getName(), field.get(source));
			field.setAccessible(accessibility);
		}

		private void processField(String name, Object value) {
			if (isArray(value.getClass())) processArray(name, value);
			else if (isList(value.getClass())) processList(name, value);
			else if (isMap(value.getClass())) processMap(name, value);
			else context.add(name, value);
		}

		private void processArray(String name, Object value) {
			processList(name, ArrayValues.of(value));
		}

		private void processList(String name, Object value) {
			List list = (List) value;
			context.add(name + Count, list.size());
			for (Object item : list) {
				context.add(name, item);
			}
		}

		private void processMap(String name, Object value) {
			final Map map = (Map) value;
			context.add(name + Count, map.keySet().size());
			for (Object key : map.keySet())
				context.add(name, new Item(key, map.get(key)));
		}

		private boolean isMap(Class<?> aClass) {
			return Map.class.isAssignableFrom(aClass);
		}

		private boolean isList(Class<?> aClass) {
			return List.class.isAssignableFrom(aClass);
		}

		private boolean isArray(Class<?> aClass) {
			return aClass.isArray();
		}
	}

}
