package tests.fixtures;

import java.util.Arrays;
import java.util.List;

public class ObjectWithList {

	private final List<Object> field1;

	public ObjectWithList(Object... values) {
		this.field1 = Arrays.asList(values);
	}

	public Object get(int index) {
		return field1.get(index);
	}

}
