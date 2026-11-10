package tests.fixtures;


import java.time.LocalDate;
import java.util.List;

public final class LibraryFixtures {
	private LibraryFixtures() {
	}

	public static Book book() {
		return new Book("Don Quixote", LocalDate.of(1605, 1, 16), "Acme", Genre.Novel, List.of(), List.of());
	}

	public static Book trial() {
		return new Book("The Trial", LocalDate.of(1925, 4, 26), "Acme", Genre.Novel, List.of(), List.of());
	}

	public static List<Chapter> chapters() {
		return List.of(new Chapter("Preface", Kind.Introduction), new Chapter("The Journey", Kind.Main),
			new Chapter("Notes", Kind.Appendix));
	}

	public static List<Edition> editions() {
		return List.of(new Edition("First edition"), new Edition("Illustrated edition"));
	}

}
