package io.quassar.templating.adapters.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

final class FrameGetters {
	private static final ClassValue<Map<String, Method>> GETTERS = new ClassValue<>() {
		@Override
		protected Map<String, Method> computeValue(Class<?> type) {
			Map<String, Method> getters = new HashMap<>();
			for (Method method : type.getMethods())
				if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()) && !method.isBridge())
					getters.putIfAbsent(method.getName(), method);
			return Map.copyOf(getters);
		}
	};

	private FrameGetters() {
	}

	static Object read(Object source, String name) {
		Method method = GETTERS.get(source.getClass()).get(name);
		if (method == null) throw new IllegalArgumentException("Unknown frame getter " + name);
		try {
			if (!method.canAccess(source)) method.setAccessible(true);
			return method.invoke(source);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not read frame method " + name, exception);
		}
	}
}
