package tests.fixtures;

import java.util.List;

public class ObjectWithTwoList {

	private final List<String> field1;
	private final List<Double> field2;

	public ObjectWithTwoList(List<String> field1, List<Double> field2) {
		this.field1 = field1;
		this.field2 = field2;
	}
}
