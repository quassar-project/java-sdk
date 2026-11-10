package io.quassar.templating.adapters.reflection;

import io.quassar.templating.annotations.FrameIdentity;
import io.quassar.templating.annotations.FrameParent;
import io.quassar.templating.annotations.FrameSlot;
import io.quassar.templating.annotations.FrameSlots;
import io.quassar.templating.annotations.FrameType;

import io.quassar.templating.ports.FrameView;
import io.quassar.templating.ports.FrameSource;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

record FrameDefinition(String type, List<Method> types, List<Method> slots, List<Method> maps,
		Method identity, Method parent, Map<String, Method> getters) {
	private static final ClassValue<FrameDefinition> DEFINITIONS = new ClassValue<>() {
		@Override
		protected FrameDefinition computeValue(Class<?> type) {
			return read(type);
		}
	};

	static FrameDefinition of(Class<?> type) {
		return DEFINITIONS.get(type);
	}

	boolean annotated() {
		return !type.isEmpty() || !types.isEmpty() || !slots.isEmpty() || !maps.isEmpty() || identity != null || parent != null;
	}

	Object identity(Object source) {
		return identity == null ? source : invoke(identity, source);
	}

	Method getter(String name) {
		Method method = getters.get(name);
		if (method == null) throw new IllegalArgumentException("Unknown frame getter " + name);
		return method;
	}

	static Object invoke(Method method, Object source) {
		try {
			return method.invoke(source);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not read frame method " + method.getName(), exception);
		}
	}

	private static FrameDefinition read(Class<?> type) {
		List<Method> methods = Arrays.stream(type.getMethods())
			.filter(method -> method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()))
			.sorted(Comparator.comparing(Method::getName)).toList();
		FrameType annotation = type.getAnnotation(FrameType.class);
		return new FrameDefinition(annotation == null ? "" : annotation.value(),
			annotated(methods, FrameType.class), annotated(methods, FrameSlot.class), annotated(methods, FrameSlots.class),
			annotated(methods, FrameIdentity.class).stream().findFirst().orElse(null),
			annotated(methods, FrameParent.class).stream().findFirst().orElse(null), getters(type, methods));
	}

	private static Map<String, Method> getters(Class<?> type, List<Method> methods) {
		if (!FrameView.class.isAssignableFrom(type) && !FrameSource.class.isAssignableFrom(type)) return Map.of();
		Map<String, Method> getters = new LinkedHashMap<>();
		for (Method method : methods)
			if (!method.isBridge()) getters.putIfAbsent(method.getName(), accessible(method));
		return Map.copyOf(getters);
	}

	private static List<Method> annotated(List<Method> methods, Class<? extends Annotation> annotation) {
		return methods.stream().filter(method -> method.isAnnotationPresent(annotation)).map(FrameDefinition::accessible).toList();
	}
	private static Method accessible(Method method) {
		method.setAccessible(true);
		return method;
	}
}
