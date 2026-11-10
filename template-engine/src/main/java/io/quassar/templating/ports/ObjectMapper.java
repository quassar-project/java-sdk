package io.quassar.templating.ports;

public interface ObjectMapper<T> {
	void map(T source, FrameBuilder context);

}
