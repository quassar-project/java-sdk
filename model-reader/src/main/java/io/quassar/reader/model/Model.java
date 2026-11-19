package io.quassar.reader.model;

import io.quassar.reader.domain.NodeSupport;

import java.io.FileNotFoundException;
import java.util.*;

public final class Model {
	private final List<Mogram> mograms;
	private final Index index;
	private final Map<String, Resource> resources;
	private final NodeSupport support;

	public Model(List<Mogram> mograms) {
		this(mograms, Map.of(), new NodeSupport(List.of()));
	}

	private Model(List<Mogram> mograms, Map<String, Resource> resources, NodeSupport support) {
		this.mograms = List.copyOf(mograms);
		this.index = Index.of(this.mograms);
		this.resources = resources;
		this.support = support.forModel(this);
	}

	private Model(Model model, Map<String, Resource> resources, NodeSupport support) {
		this.mograms = model.mograms;
		this.index = model.index;
		this.resources = resources;
		this.support = support.forModel(this);
	}

	public Model withResources(Map<String, byte[]> resources) {
		Map<String, Resource> copy = new TreeMap<>();
		resources.forEach((name, bytes) -> copy.put(name, new Resource(name, bytes)));
		return new Model(this, Collections.unmodifiableMap(copy), support);
	}

	public Model withMograms(List<Mogram> mograms) {
		return new Model(mograms, resources, support);
	}

	public Model withSupport(NodeSupport support) {
		if (this.support.sameSchema(support)) return this;
		return new Model(this, resources, support);
	}

	NodeSupport support() {
		return support;
	}

	public Set<String> resourceNames() {
		return resources.keySet();
	}

	public List<Resource> resources() {
		return List.copyOf(resources.values());
	}

	public Resource resource(String name) throws FileNotFoundException {
		Resource resource = resources.get(Objects.requireNonNull(name));
		if (resource == null) throw new FileNotFoundException("Resource not found: " + name);
		return resource;
	}

	public Resource resource(Value value) {
		if (value instanceof Value.Text text) return resources.get(text.value());
		if (value instanceof Value.Reference reference) return resources.get(reference.path());
		return null;
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
		private final List<Mogram> roots;
		private final Map<Mogram, Position> positions;

		private Index(Builder builder) {
			mograms = List.copyOf(builder.mograms);
			Map<String, List<Mogram>> paths = new LinkedHashMap<>();
			builder.byPath.forEach((path, matches) -> paths.put(path, List.copyOf(matches)));
			byPath = Map.copyOf(paths);
			positions = Collections.unmodifiableMap(new IdentityHashMap<>(builder.positions));
			roots = mograms.stream().filter(mogram -> positions.get(mogram).parent() == null).toList();
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
			List<Mogram> matches = findPath(roots, reference);
			if (!matches.isEmpty()) return matches;
			return find(reference).stream().filter(Mogram::anonymous).toList();
		}

        private List<Mogram> findInScope(String reference, Mogram scope) {
            if (!positions.containsKey(scope)) return List.of();
            return findPath(scope.children(), reference);
        }

        private static List<Mogram> findPath(List<Mogram> candidates, String reference) {
            List<Mogram> matches = candidates;
            String[] parts = reference.split("\\.", -1);
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) return List.of();
                String identifier = parts[i];
                matches = matches.stream().filter(mogram -> identifier.equals(mogram.identifier())).toList();
                if (i < parts.length - 1) matches = matches.stream().flatMap(mogram -> mogram.children().stream()).toList();
            }
            return matches;
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
			if (!mogram.identifier().isEmpty()) {
				String key = mogram.anonymous() ? identifiedPath(mogram, prefix) : path;
				byPath.computeIfAbsent(key, ignored -> new ArrayList<>()).add(mogram);
				if (mogram.anonymous() && !key.equals(mogram.identifier()))
					byPath.computeIfAbsent(mogram.identifier(), ignored -> new ArrayList<>()).add(mogram);
			}
			for (Mogram child : mogram.children()) add(child, mogram, path);
		}

		private String pathOf(Mogram mogram, String prefix) {
			if (mogram.anonymous()) return prefix;
			return identifiedPath(mogram, prefix);
		}

		private String identifiedPath(Mogram mogram, String prefix) {
			if (prefix.isEmpty()) return mogram.identifier();
			return prefix + "." + mogram.identifier();
		}
	}
}
