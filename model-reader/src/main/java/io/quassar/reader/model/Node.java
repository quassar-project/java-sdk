package io.quassar.reader.model;

import io.quassar.templating.ports.FrameSource;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class Node extends FrameSource {
	protected final Mogram mogram;
	protected final Model model;

	protected Node(Mogram mogram, Model model) {
		this.mogram = mogram;
		this.model = model;
	}

	public final String identifier() {
		return mogram.identifier();
	}

	public final boolean isNull() {
		return mogram.isNull();
	}

	public final boolean isNotNull() {
		return !isNull();
	}

	public final List<String> types() {
		return isNull() ? List.of() : modelTypes();
	}

	public final <T extends Node> T owner(Class<T> type) {
		Mogram parent = model.parentOf(mogram);
		while (!parent.isNull()) {
			T candidate = ownerView(type, parent);
			if (candidate.isNotNull()) return candidate;
			parent = model.parentOf(parent);
		}
		return ownerView(type, Mogram.NULL);
	}

	protected <T extends Node> T ownerView(Class<T> type, Mogram owner) {
		throw new IllegalArgumentException("Unknown node type: " + type.getName());
	}

	protected List<String> modelTypes() {
		return Stream.concat(Stream.of(mogram.type()), mogram.facets().stream()).distinct().toList();
	}

	@Override
	public final boolean equals(Object other) {
		if (!(other instanceof Node node) || getClass() != node.getClass()) return false;
		return mogram.equals(node.mogram) && model.equals(node.model);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(mogram, model);
	}
}
