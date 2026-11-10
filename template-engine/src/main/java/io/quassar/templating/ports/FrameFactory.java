package io.quassar.templating.ports;

import io.quassar.templating.model.Frame;

import java.util.Map;

@FunctionalInterface
public interface FrameFactory {
	Frame create(Object object, Map<Class<?>, ObjectMapper> mappers);
}
