package tests.fixtures;

public class PolymorphicProvider {

	private interface InterfaceA {
	}

	interface InterfaceB extends InterfaceA {
	}

	public interface InterfaceC {
	}

	public static class ClassA {
	}

	public static class ClassB extends ClassA {
		private final double field1 = 0.0;
		private final double field2 = 1.0;
	}

}
