package tests.fixtures;

import java.time.LocalDate;
import java.util.List;

public record Book(String title, LocalDate published, String publisher, Genre genre,
		List<Chapter> chapters, List<Edition> editions) {
	public Book {
		chapters = List.copyOf(chapters);
		editions = List.copyOf(editions);
	}

	public Book withChapters(List<Chapter> chapters) {
		return new Book(title, published, publisher, genre, chapters, editions);
	}

	public Book withEditions(List<Edition> editions) {
		return new Book(title, published, publisher, genre, chapters, editions);
	}
}
