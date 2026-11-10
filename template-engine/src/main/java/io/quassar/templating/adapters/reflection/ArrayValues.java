package io.quassar.templating.adapters.reflection;

import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.IntStream;

final class ArrayValues {
	private ArrayValues() {
	}

	static List<Object> of(Object array) {
		return IntStream.range(0, Array.getLength(array)).mapToObj(index -> Array.get(array, index)).toList();
	}
}
