package io.quassar.templating.model;

import java.util.Iterator;
import java.util.List;

public interface Frame {
	boolean is(String type);

	Iterator<Frame> frames(String slot);

	boolean contains(String slot);

	Frame container();

	void container(Frame container);

	Object value();

	default List<Frame> children() {
		return List.of();
	}

}
