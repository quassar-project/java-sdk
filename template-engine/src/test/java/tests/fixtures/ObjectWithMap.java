package tests.fixtures;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObjectWithMap {
	private final Map<Object, Object> map = new LinkedHashMap<>();

	public ObjectWithMap() {
	}

	public ObjectWithMap add(Object key, Object value) {
		map.put(key, value);
		return this;
	}
}
