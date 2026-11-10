package tests.fixtures;

public class TransientAttributeObject {

	private final String field1;
	private transient final Double field2;

	public TransientAttributeObject(String field1, Double field2) {
		this.field1 = field1;
		this.field2 = field2;
	}

	public String getField1() {
		return field1;
	}
}
