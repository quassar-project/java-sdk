package io.quassar.templating.model.frames;

import io.quassar.templating.model.Frame;

import java.util.*;

import static java.util.Collections.emptyIterator;
import static java.lang.String.join;

public final class CompositeFrame implements Frame {
	private final List<String> types;
	private Frame container;
	private final Map<String, List<Frame>> slots;

	public CompositeFrame(List<String> types, Map<String, List<Frame>> slots) {
		this.types = types;
		this.slots = slots;
		slots.values().forEach(s -> new ArrayList<>(s).forEach(f -> f.container(CompositeFrame.this)));
	}

	@Override
	public boolean is(String type) {
		return this.types.stream().anyMatch(t -> t.equals(type));
	}

	@Override
	public boolean contains(String slot) {
		return slots.containsKey(slot.toLowerCase());
	}

	@Override
	public Frame container() {
		return container;
	}

	public void container(Frame container) {
		this.container = container;
	}

	@Override
	public Iterator<Frame> frames(String slot) {
		return contains(slot) ? slots.get(slot.toLowerCase()).iterator() : emptyIterator();
	}

	@Override
	public List<Frame> children() {
		return slots.values().stream().flatMap(Collection::stream).toList();
	}

	public Map<String, List<Frame>> slots() {
		return slots;
	}

	@Override
	public Object value() {
		return null;
	}

	@Override
	public int hashCode() {
		int h = types.hashCode();
		for (String slot : sortedSlots())
			h = (31 * h + hashCodeOf(slot)) >>> 1;
		return h;
	}

	private List<String> sortedSlots() {
		List<String> list = new ArrayList<>(slots.keySet());
		list.sort(Comparator.naturalOrder());
		return list;
	}

	private int hashCodeOf(String slot) {
		Iterator<Frame> frames = frames(slot);
		int h = slot.hashCode();
		while (frames.hasNext()) {
			Frame frame = frames.next();
			h = (31 * h + frame.hashCode()) >>> 1;
		}
		return h;
	}

	@Override
	public String toString() {
		return "Frame <" + join(",", slots.keySet()) + ">";
	}

}
