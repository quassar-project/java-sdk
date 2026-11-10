package io.quassar.templating.adapters.reflection;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.frames.CompositeFrame;
import io.quassar.templating.ports.FrameView;
import io.quassar.templating.ports.FrameView.Getter;
import io.quassar.templating.ports.FrameView.ValueSlot;
import io.quassar.templating.ports.ObjectMapper;

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

	Frame read(Object source, FrameView view) {
		Frame frame = frame(source, view);
		return frame == null ? new CompositeFrame(List.of(), Map.of()) : frame;
	}

	private Frame frame(Object source) {
		if (source == null) return null;
		if (source instanceof Frame frame) return frame;
		FrameView view = ObjectFrameFactory.isMapped(source, mappers) ? null : FrameView.from(source);
		return view == null ? new ObjectFrameFactory().create(source, mappers) : frame(source, view);
	}

	private Frame frame(Object source, FrameView view) {
		Object identity = view.frameIdentity();
		if (identity == null) return null;
		return frames.computeIfAbsent(identity, key -> new MethodFrame(source, view));
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
		private final FrameView view;
		private Map<String, List<Frame>> slots;

		private MethodFrame(Object source, FrameView view) {
			this.source = source;
			this.view = view;
		}

		@Override
		public boolean is(String type) {
			return view.frameTypes().stream().anyMatch(type::equalsIgnoreCase);
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
			return frame(view.frameParent());
		}

		@Override
		public void container(Frame container) {
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
			for (FrameView.Slot slot : view.frameSlots())
				slots.computeIfAbsent(normalize(slot.name()), key -> new ArrayList<>()).addAll(values(read(slot), this));
			return slots;
		}

		private Object read(FrameView.Slot slot) {
			if (slot instanceof Getter getter) return getter.projection().apply(FrameGetters.read(source, getter.method()));
			return ((ValueSlot) slot).value();
		}
	}
}
