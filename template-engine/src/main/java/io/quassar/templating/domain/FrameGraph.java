package io.quassar.templating.domain;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.frames.CompositeFrame;
import io.quassar.templating.ports.FrameView;
import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.FrameFactory;
import io.quassar.templating.ports.SlotReader;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FrameGraph {
	private final Map<Object, Frame> frames = new IdentityHashMap<>();
	private final Map<Class<?>, ObjectMapper> mappers;
	private final FrameFactory fallback;
	private final SlotReader reader;

	public FrameGraph(FrameFactory fallback, SlotReader reader, Map<Class<?>, ObjectMapper> mappers) {
		this.mappers = mappers;
		this.fallback = fallback;
		this.reader = reader;
	}

	public Frame read(Object source, FrameView view) {
		Frame frame = frame(source, view);
		return frame == null ? new CompositeFrame(List.of(), Map.of()) : frame;
	}

	private Frame frame(Object source) {
		if (source == null) return null;
		if (source instanceof Frame frame) return frame;
		FrameView view = mappers.keySet().stream().anyMatch(type -> type.isInstance(source)) ? null : FrameView.from(source);
		return view == null ? fallback.create(source, mappers) : frame(source, view);
	}

	private Frame frame(Object source, FrameView view) {
		Object identity = view.frameIdentity();
		if (identity == null) return null;
		return frames.computeIfAbsent(identity, key -> new MethodFrame(source, view));
	}

	private List<Frame> values(Object source, FrameView.Slot slot, Frame owner) {
		List<Frame> values = new ArrayList<>();
		for (Object value : reader.read(source, slot)) {
			Frame frame = frame(value);
			if (frame == null) continue;
			frame.container(owner);
			values.add(frame);
		}
		return List.copyOf(values);
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
				slots.computeIfAbsent(normalize(slot.name()), key -> new ArrayList<>()).addAll(values(source, slot, this));
			return slots;
		}

	}
}
