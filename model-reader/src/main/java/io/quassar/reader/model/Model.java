package io.quassar.reader.model;

import java.util.*;

public final class Model {
	private final List<Mogram> mograms;
	private final Index index;

	public Model(List<Mogram> mograms) {
		this.mograms = List.copyOf(mograms);
		this.index = Index.of(this.mograms);
	}

	public List<Mogram> mograms() {
		return mograms;
	}

	public List<Mogram> allMograms() {
		return index.mograms();
	}

	public MogramQuery select() {
		return new MogramQuery(this, allMograms());
	}

	public MogramQuery roots() {
		return new MogramQuery(this, mograms);
	}

	public MogramQuery select(Mogram mogram) {
		return new MogramQuery(this, mogram.isNull() ? List.of() : List.of(mogram));
	}

	public Mogram get(String path) {
		return find(path).stream().findFirst().orElse(Mogram.NULL);
	}

	public Mogram parentOf(Mogram mogram) {
		Mogram parent = index.parentOf(mogram);
		return parent == null ? Mogram.NULL : parent;
	}

	public List<Mogram> find(String path) {
		return index.find(path);
	}

	public Mogram get(Value value) {
		return value instanceof Value.Reference reference ? get(reference.path()) : Mogram.NULL;
	}

	public Mogram get(Value value, Mogram context) {
		if (!(value instanceof Value.Reference reference)) return Mogram.NULL;
		return index.find(reference.path(), context).stream().findFirst().orElse(Mogram.NULL);
	}

	static final class Index {
		private final List<Mogram> mograms;
		private final Map<String, List<Mogram>> byPath;
		private final Map<Mogram, Position> positions;

		private Index(Builder builder) {
			mograms = List.copyOf(builder.mograms);
			Map<String, List<Mogram>> paths = new LinkedHashMap<>();
			builder.byPath.forEach((path, matches) -> paths.put(path, List.copyOf(matches)));
			byPath = Map.copyOf(paths);
			positions = Collections.unmodifiableMap(new IdentityHashMap<>(builder.positions));
		}

		static Index of(List<Mogram> roots) {
			Builder builder = new Builder();
			for (Mogram root : roots) builder.add(root, null, "");
			return new Index(builder);
		}

		List<Mogram> mograms() {
			return mograms;
		}

		List<Mogram> find(String path) {
			return byPath.getOrDefault(path, List.of());
		}

		Mogram parentOf(Mogram mogram) {
			Position position = positions.get(mogram);
			return position == null ? null : position.parent();
		}

		List<Mogram> find(String reference, Mogram context) {
			for (Mogram scope = context; scope != null; scope = parentOf(scope)) {
				List<Mogram> matches = findInScope(reference, scope);
				if (!matches.isEmpty()) return matches;
			}
			return find(reference);
		}

		private List<Mogram> findInScope(String reference, Mogram scope) {
			Position position = positions.get(scope);
			if (position == null || position.path().isEmpty()) return List.of();
			return find(position.path() + "." + reference);
		}

		private record Position(String path, Mogram parent) {
		}
	}

	private static final class Builder {
		private final List<Mogram> mograms = new ArrayList<>();
		private final Map<String, List<Mogram>> byPath = new LinkedHashMap<>();
		private final Map<Mogram, Index.Position> positions = new IdentityHashMap<>();

		private void add(Mogram mogram, Mogram parent, String prefix) {
			String path = pathOf(mogram, prefix);
			mograms.add(mogram);
			positions.put(mogram, new Index.Position(path, parent));
			if (!mogram.identifier().isEmpty()) byPath.computeIfAbsent(path, key -> new ArrayList<>()).add(mogram);
			for (Mogram child : mogram.children()) add(child, mogram, path);
		}

		private String pathOf(Mogram mogram, String prefix) {
			if (mogram.identifier().isEmpty()) return prefix;
			if (prefix.isEmpty()) return mogram.identifier();
			return prefix + "." + mogram.identifier();
		}
	}
}
