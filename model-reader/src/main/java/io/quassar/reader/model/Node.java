package io.quassar.reader.model;

import io.quassar.templating.ports.FrameSource;
import io.quassar.reader.domain.NodeFactory;

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

	public final boolean is(String type) {
		return types().stream().anyMatch(name -> name.equals(type) || name.endsWith("." + type));
	}

	public final boolean is(Class<? extends Node> type) {
		if (type.equals(Node.class)) return isNotNull();
		return types().contains(NodeFactory.type(type));
	}

	public final <T extends Node> T ancestor(Class<T> type) {
		Node parent = parent();
		while (parent.isNotNull()) {
			T candidate = parent.as(type);
			if (candidate.isNotNull()) return candidate;
			parent = parent.parent();
		}
		return parent.as(type);
	}

	public abstract <T extends Node> T as(Class<T> type);

	private Node parent() {
		return wrap(model.parentOf(mogram));
	}

	protected abstract Node wrap(Mogram mogram);

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
