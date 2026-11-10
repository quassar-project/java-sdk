package io.quassar.templating.model.predicates;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.Trigger;
import io.quassar.templating.model.outputs.Placeholder;

import java.util.Iterator;

public class AttributePredicate implements Predicate {
	private final String attribute;
	private final boolean isThis;
	private final Object value;

	public AttributePredicate(String attribute) {
		this(attribute, null);
	}

	public AttributePredicate(String attribute, Object value) {
		this.isThis = attribute.equals(Placeholder.THIS) || attribute.isEmpty();
		this.attribute = this.isThis ? "" : attribute.toLowerCase();
		this.value = value;
	}

	public String attribute() {
		return attribute;
	}

	public Object value() {
		return value;
	}

	@Override
	public boolean evaluate(Trigger trigger) {
		return checkAttribute(trigger.frame()) && checkValue(trigger.frame());
	}

	private boolean checkAttribute(Frame frame) {
		return isThis ? frame != null : frame.contains(attribute);
	}

	private boolean checkValue(Frame frame) {
		if (value == null) return true;
		return isThis ? equalTo(frame.value()) : isIn(frame);
	}

	private boolean isIn(Frame frame) {
		Iterator<Frame> frames = frame.frames(attribute);
		while (frames.hasNext())
			if (equalTo(frames.next().value())) return true;
		return false;
	}

	private boolean equalTo(Object o) {
		if (o == null) return false;
		return this == o || o.toString().equalsIgnoreCase(value.toString());
	}

	@Override
	public String toString() {
		return "attribute(" + attribute + (value != null ? ", " + value : "") + ")";
	}
}
