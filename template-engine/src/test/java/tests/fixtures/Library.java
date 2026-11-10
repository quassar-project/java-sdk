package tests.fixtures;

import java.util.List;

public record Library(String name, Author librarian, List<Book> books) {
	public Library {
		books = List.copyOf(books);
	}
}
