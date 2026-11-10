package example;

import example.templates.BookSummaryTemplate;
import example.model.LibraryModel;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.util.List;

public final class Library {
	public static String summary(String title) {
		Mogram book = new Mogram("Book", "Example", List.of(new Property("title", new Value.Text(title))), List.of(), List.of());
		var library = new LibraryModel(new Model(List.of(book)));
		return BookSummaryTemplate.render(library.books().get(0));
	}
}
