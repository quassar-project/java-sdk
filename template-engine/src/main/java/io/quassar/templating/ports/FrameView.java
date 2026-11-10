package io.quassar.templating.ports;

import java.util.List;
import java.util.function.UnaryOperator;

public interface FrameView {
	static FrameView from(Object source) {
		if (source instanceof FrameSource framed) return framed.frameView();
		return source instanceof FrameView view ? view : null;
	}

	Object frameIdentity();

	Object frameParent();

	List<String> frameTypes();

	List<Slot> frameSlots();

	sealed interface Slot permits Getter, ValueSlot {
		String name();
	}

	record Getter(String name, String method, UnaryOperator<Object> projection) implements Slot {
		public Getter(String name, String method) {
			this(name, method, UnaryOperator.identity());
		}
	}

	record ValueSlot(String name, Object value) implements Slot {
	}
}
