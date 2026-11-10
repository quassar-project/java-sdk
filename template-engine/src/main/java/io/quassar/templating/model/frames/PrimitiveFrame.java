package io.quassar.templating.model.frames;

import io.quassar.templating.model.Frame;

import java.util.*;

import static java.util.Collections.emptyIterator;

public final class PrimitiveFrame implements Frame {
	private final Object value;
	private final String type;
	private Frame container;

	public PrimitiveFrame(Object value) {
		this.value = value;
		this.type = value.getClass().getSimpleName().toLowerCase();
	}

	@Override
	public boolean is(String type) {
		return type.equals(this.type);
	}

	@Override
	public Iterator<Frame> frames(String slot) {
		return emptyIterator();
	}

	@Override
	public boolean contains(String slot) {
		return false;
	}

	public void container(Frame container) {
		this.container = container;
	}

	@Override
	public Frame container() {
		return container;
	}

	@Override
	public Object value() {
		return value;
	}

	@Override
	public int hashCode() {
		return type.hashCode() + 31 * (value != null ? value.hashCode() : 0);
	}

	@Override
	public String toString() {
		return "Frame <" + value + ": " + type + ">";
	}

}
