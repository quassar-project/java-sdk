package io.quassar.templating;

import io.quassar.templating.adapters.reflection.ObjectFrameFactory;
import io.quassar.templating.model.Frame;

import java.util.Map;

public final class Frames {
	private Frames() {
	}

	public static Frame from(Object object) {
		return new ObjectFrameFactory().create(object, Map.of());
	}
}
