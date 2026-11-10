package io.quassar.templating.adapters.reflection;

import io.quassar.templating.annotations.FrameSlot;
import io.quassar.templating.model.Frame;
import io.quassar.templating.model.frames.CompositeFrame;
import io.quassar.templating.ports.FrameView;
import io.quassar.templating.ports.FrameView.Getter;
import io.quassar.templating.ports.FrameView.ValueSlot;
import io.quassar.templating.ports.ObjectMapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MethodFrames {
	private final Map<Object, Frame> frames = new IdentityHashMap<>();
	private final Map<Class<?>, ObjectMapper> mappers;

	MethodFrames(Map<Class<?>, ObjectMapper> mappers) {
		this.mappers = mappers;
	}

	Frame read(Object source) {
		Frame frame = frame(source);
		return frame == null ? new CompositeFrame(List.of(), Map.of()) : frame;
	}

	private Frame frame(Object source) {
		if (source == null) return null;
		if (source instanceof Frame frame) return frame;
		if (ObjectFrameFactory.isMapped(source, mappers)) return new ObjectFrameFactory().create(source, mappers);
		FrameView view = FrameView.from(source);
		FrameDefinition definition = FrameDefinition.of(source.getClass());
		if (view == null && !definition.annotated())
			return new ObjectFrameFactory().create(source, mappers);
		Object identity = view == null ? definition.identity(source) : view.frameIdentity();
		if (identity == null) return null;
		return frames.computeIfAbsent(identity, key -> new MethodFrame(source, definition, view));
	}

	private List<Frame> values(Object value, Frame owner) {
		if (value == null) return List.of();
		if (value instanceof Iterable<?> iterable) {
			List<Frame> frames = new ArrayList<>();
			for (Object item : iterable) frames.addAll(values(item, owner));
			return List.copyOf(frames);
		}
		if (value.getClass().isArray()) return values(ArrayValues.of(value), owner);
		Frame frame = frame(value);
		if (frame == null) return List.of();
		frame.container(owner);
		return List.of(frame);
	}

	private String normalize(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	private final class MethodFrame implements Frame {
		private final Object source;
		private final FrameDefinition definition;
		private final FrameView view;
		private Map<String, List<Frame>> slots;
		private Frame container;

		private MethodFrame(Object source, FrameDefinition definition, FrameView view) {
			this.source = source;
			this.definition = definition;
			this.view = view;
		}

		@Override
		public boolean is(String type) {
			if (view != null)
				return view.frameTypes().stream().anyMatch(type::equalsIgnoreCase);
			if (definition.type().equalsIgnoreCase(type)) return true;
			for (Method method : definition.types()) {
				Object value = FrameDefinition.invoke(method, source);
				if (value instanceof Iterable<?> types) {
					for (Object item : types) if (item.toString().equalsIgnoreCase(type)) return true;
				} else if (value != null && value.toString().equalsIgnoreCase(type)) return true;
			}
			return false;
		}

		@Override
		public Iterator<Frame> frames(String slot) {
			return slots().getOrDefault(normalize(slot), List.of()).iterator();
		}

		@Override
		public boolean contains(String slot) {
			return slots().containsKey(normalize(slot));
		}

		@Override
		public Frame container() {
			if (view != null) return frame(view.frameParent());
			return definition.parent() == null ? container : frame(FrameDefinition.invoke(definition.parent(), source));
		}

		@Override
		public void container(Frame container) {
			this.container = container;
		}

		@Override
		public Object value() {
			return null;
		}

		@Override
		public List<Frame> children() {
			return slots().values().stream().flatMap(List::stream).toList();
		}

		private Map<String, List<Frame>> slots() {
			if (slots != null) return slots;
			slots = new LinkedHashMap<>();
			if (view != null) {
				for (FrameView.Slot slot : view.frameSlots())
					slots.computeIfAbsent(normalize(slot.name()), key -> new ArrayList<>()).addAll(values(read(slot), this));
				return slots;
			}
			for (Method method : definition.slots()) {
				String name = method.getAnnotation(FrameSlot.class).value();
				put(name.isEmpty() ? method.getName() : name, FrameDefinition.invoke(method, source));
			}
			for (Method method : definition.maps()) {
				Map<?, ?> values = (Map<?, ?>) FrameDefinition.invoke(method, source);
				for (var entry : values.entrySet()) put(entry.getKey().toString(), entry.getValue());
			}
			return slots;
		}

		private Object read(FrameView.Slot slot) {
			if (slot instanceof Getter getter) {
				Object value = FrameDefinition.invoke(definition.getter(getter.method()), source);
				return getter.projection().apply(value);
			}
			return ((ValueSlot) slot).value();
		}

		private void put(String name, Object value) {
			List<Frame> frames = values(value, this);
			slots.put(normalize(name), frames);
		}
	}
}
