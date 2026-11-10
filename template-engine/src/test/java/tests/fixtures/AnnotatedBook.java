package tests.fixtures;

import io.quassar.templating.annotations.FrameIdentity;
import io.quassar.templating.annotations.FrameParent;
import io.quassar.templating.annotations.FrameSlot;
import io.quassar.templating.annotations.FrameSlots;
import io.quassar.templating.annotations.FrameType;

import java.util.List;
import java.util.Map;

@FrameType("Book")
public record AnnotatedBook(@FrameIdentity Book source) {
	@FrameSlot
	public String title() {
		return source.title();
	}

	@FrameSlot("identifier")
	public String id() {
		return "DonQuixote";
	}

	@FrameSlots
	public Map<String, Object> properties() {
		return Map.of("publisher", source.publisher(), "tags", List.of("novel", "classic"), "notes", List.of());
	}

	@FrameType
	public List<String> facets() {
		return List.of("Printed");
	}

	@FrameParent
	public Object parent() {
		return null;
	}

	@FrameSlot
	public AnnotatedBook related() {
		return new AnnotatedBook(source);
	}
}
