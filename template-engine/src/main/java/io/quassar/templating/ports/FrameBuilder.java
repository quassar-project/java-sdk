package io.quassar.templating.ports;

public interface FrameBuilder {
	FrameBuilder add(String type);

	boolean is(String type);

	FrameBuilder add(String slot, Object object);

	boolean contains(String slot);

	int slots();
}
