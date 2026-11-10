package io.quassar.templating.adapters.reflection;

import io.quassar.templating.model.Frame;
import io.quassar.templating.ports.ObjectMapper;
import io.quassar.templating.ports.FrameFactory;
import io.quassar.templating.ports.FrameSource;
import io.quassar.templating.ports.FrameView;

import java.util.Map;

public final class ObjectFrameFactory implements FrameFactory {
	@Override
	public Frame create(Object object, Map<Class<?>, ObjectMapper> mappers) {
		if (object instanceof Frame frame) return frame;
		if (object instanceof ReflectiveFrameBuilder builder) return builder.toFrame();
		if (!isMapped(object, mappers) && (object instanceof FrameSource || object instanceof FrameView
			|| FrameDefinition.of(object.getClass()).annotated()))
			return new MethodFrames(mappers).read(object);
		return new ReflectiveFrameBuilder().put(mappers).append(object).toFrame();
	}

	static boolean isMapped(Object object, Map<Class<?>, ObjectMapper> mappers) {
		return mappers.keySet().stream().anyMatch(type -> type.isInstance(object));
	}
}
