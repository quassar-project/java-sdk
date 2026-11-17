package io.quassar.reader.domain;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class NodeFactory {
	private static final ClassValue<Definition> DEFINITIONS = new ClassValue<>() {
		@Override
		protected Definition computeValue(Class<?> type) {
			try {
				var constructor = type.asSubclass(Node.class).getDeclaredConstructor(Mogram.class, Model.class);
				var field = type.getDeclaredField("TYPE");
				constructor.setAccessible(true);
				field.setAccessible(true);
				return new Definition((String) field.get(null), constructor);
			} catch (ReflectiveOperationException exception) {
				throw new IllegalArgumentException("Unknown node type: " + type.getName(), exception);
			}
		}
	};

	private NodeFactory() {
	}

	public static String type(Class<? extends Node> type) {
		return DEFINITIONS.get(type).name();
	}

	public static <T extends Node> T create(Class<T> type, Mogram mogram, Model model) {
		try {
			return type.cast(DEFINITIONS.get(type).constructor().newInstance(mogram, model));
		} catch (InstantiationException | IllegalAccessException exception) {
			throw new IllegalStateException("Could not create node: " + type.getName(), exception);
		} catch (InvocationTargetException exception) {
			throw new IllegalStateException("Could not create node: " + type.getName(), exception.getCause());
		}
	}

	private record Definition(String name, Constructor<? extends Node> constructor) {
	}
}
