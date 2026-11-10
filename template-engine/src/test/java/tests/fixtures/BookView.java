package tests.fixtures;

import io.quassar.templating.ports.FrameView;

import java.util.List;

public record BookView(Book source) implements FrameView {
	public String title() {
		return source.title();
	}

	@Override
	public Object frameIdentity() {
		return source;
	}

	@Override
	public Object frameParent() {
		return null;
	}

	@Override
	public List<String> frameTypes() {
		return List.of("Book", "Printed");
	}

	@Override
	public List<Slot> frameSlots() {
		return List.of(
			new Getter("title", "title"),
			new ValueSlot("identifier", "DonQuixote"),
			new ValueSlot("publisher", source.publisher()),
			new ValueSlot("tags", List.of("novel", "classic")),
			new ValueSlot("notes", List.of()),
			new ValueSlot("related", new BookView(source)));
	}
}
