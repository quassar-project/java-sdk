package tests.fixtures;

import java.util.List;

public record Chapter(String title, Kind kind, List<Chapter> children) {
	public Chapter {
		children = List.copyOf(children);
	}

	public Chapter(String title, Kind kind) {
		this(title, kind, List.of());
	}
}
