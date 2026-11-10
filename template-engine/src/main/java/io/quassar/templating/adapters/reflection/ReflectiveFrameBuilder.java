package io.quassar.templating.adapters.reflection;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.frames.CompositeFrame;
import io.quassar.templating.model.frames.PrimitiveFrame;
import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.FrameBuilder;

import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public final class ReflectiveFrameBuilder implements FrameBuilder {
	private final List<String> types;
	private final Map<String, List<Frame>> slots;
	private Map<Class<?>, ObjectMapper> mappers;
	private Object value;

	public ReflectiveFrameBuilder() {
		this.types = new ArrayList<>();
		this.slots = new HashMap<>();
		this.mappers = new HashMap<>();
	}

	public ReflectiveFrameBuilder(String... types) {
		this.types = toLowerCase(asList(types));
		this.slots = new HashMap<>();
		this.mappers = new HashMap<>();
	}

	public static ReflectiveFrameBuilder from(FrameBuilder context) {
		ReflectiveFrameBuilder builder = new ReflectiveFrameBuilder();
		if (context instanceof ReflectiveFrameBuilder) builder.mappers = ((ReflectiveFrameBuilder) context).mappers;
		if (context instanceof WrapBuilder) builder.mappers = ((WrapBuilder) context).builder.mappers;
		return builder;
	}

	private static boolean isPrimitive(Object object) {
		return isPrimitive(object.getClass());
	}

	private static boolean isPrimitive(Class<?> aClass) {
		return aClass.isPrimitive() ||
				String.class.isAssignableFrom(aClass) ||
				Number.class.isAssignableFrom(aClass) ||
				Boolean.class.isAssignableFrom(aClass) ||
				Temporal.class.isAssignableFrom(aClass) ||
				Enum.class.isAssignableFrom(aClass) ||
				Character.class.isAssignableFrom(aClass);
	}

	private List<String> toLowerCase(List<String> list) {
		return list.stream().map(String::toLowerCase).collect(toList());
	}

	public boolean is(String type) {
		return types.contains(type);
	}

	public ReflectiveFrameBuilder add(String type) {
		types.add(type.toLowerCase());
		return this;
	}

	public boolean contains(String slot) {
		return slots.containsKey(slot.toLowerCase());
	}

	@Override
	public int slots() {
		return slots.size();
	}

	@Override
	public ReflectiveFrameBuilder add(String slot, Object object) {
		if (object == null) return this;
		if (object.getClass().isArray())
			objectsIn(object).forEach(o -> get(slot).add(frameOf(o)));
		else
			get(slot).add(frameOf(object));
		return this;
	}

	private Stream<Object> objectsIn(Object object) {
		return ArrayValues.of(object).stream();
	}

	public ReflectiveFrameBuilder append(Object object) {
		addValueOf(object);
		addTypesOf(object);
		addSlotsOf(object);
		return this;
	}

	public <T> ReflectiveFrameBuilder put(Class<T> aClass, ObjectMapper<T> mapper) {
		mappers.put(aClass, mapper);
		return this;
	}

	public ReflectiveFrameBuilder put(Map<Class<?>, ObjectMapper> mappers) {
		this.mappers = mappers;
		return this;
	}

	public Frame toFrame() {
		return value != null ? new PrimitiveFrame(value) : new CompositeFrame(types, slots);
	}

	private void addValueOf(Object object) {
		if (!isPrimitive(object)) return;
		this.value = object;
	}

	private void addTypesOf(Object object) {
		if (isPrimitive(object)) return;
		this.types.addAll(typesOf(object));
	}

	private void addSlotsOf(Object object) {
		if (isPrimitive(object)) return;
		mapperFor(object).map(object, context());
	}

	private List<Frame> get(String slot) {
		slot = slot.toLowerCase();
		if (!slots.containsKey(slot)) slots.put(slot, new ArrayList<>());
		return slots.get(slot);
	}

	private Frame frameOf(Object object) {
		if (isPrimitive(object)) return new PrimitiveFrame(object);
		if (object instanceof Frame) return (Frame) object;
		if (object instanceof ReflectiveFrameBuilder) return ((ReflectiveFrameBuilder) object).toFrame();
		return build(object);
	}

	private Frame build(Object object) {
		return new ObjectFrameFactory().create(object, mappers);
	}

	@SuppressWarnings("unchecked")
	private ObjectMapper<Object> mapperFor(Object object) {
		List<ObjectMapper> mappers = classesOf(object).stream().map(this::mapperFor).collect(toList());
		for (ObjectMapper mapper : mappers)
			if (mapper != null) return mapper;
		return new ReflectionMapper<>();
	}

	private ObjectMapper mapperFor(Class<?> aClass) {
		return mappers.getOrDefault(aClass, null);
	}

	private List<String> typesOf(Object object) {
		return classesOf(object).stream()
				.map(Class::getSimpleName)
				.map(String::toLowerCase)
				.collect(toList());
	}

	private List<Class<?>> classesOf(Object object) {
		return classesOf(object.getClass());
	}

	private FrameBuilder context() {
		return new WrapBuilder();
	}

	private List<Class<?>> classesOf(Class<?> aClass) {
		List<Class<?>> types = new ArrayList<>();
		if (aClass == null) return types;
		if (!aClass.getSimpleName().isEmpty()) types.add(aClass);
		types.addAll(classesOf(aClass.getSuperclass()));
		types.addAll(interfacesOf(aClass));
		return types;
	}

	private List<Class<?>> interfacesOf(Class<?> aClass) {
		List<Class<?>> interfaces = new ArrayList<>();
		for (Class<?> aInterface : aClass.getInterfaces()) interfaces.addAll(classesOf(aInterface));
		return interfaces;
	}

	private class WrapBuilder implements FrameBuilder {
		private final ReflectiveFrameBuilder builder = ReflectiveFrameBuilder.this;

		@Override
		public FrameBuilder add(String type) {
			builder.add(type);
			return this;
		}

		@Override
		public boolean is(String type) {
			return builder.is(type);
		}

		@Override
		public FrameBuilder add(String slot, Object objects) {
			builder.add(slot, objects);
			return this;
		}

		@Override
		public boolean contains(String slot) {
			return builder.contains(slot);
		}

		@Override
		public int slots() {
			return builder.slots();
		}
	}
}
