package io.quassar.reader.model;

import io.quassar.templating.ports.FrameSource;
import io.quassar.reader.domain.NodeFactory;
import io.quassar.reader.domain.NodeSupport;
import io.quassar.templating.ports.FrameView;

import java.util.List;
import java.util.Objects;

public abstract class Node extends FrameSource {
	protected final Mogram mogram;
	protected final Model model;
	private final NodeSupport support;

	protected Node(Mogram mogram, Model model, NodeSupport support) {
		this.mogram = mogram;
		this.model = model;
		this.support = support;
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
		return isNull() ? List.of() : support.types(mogram, model);
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

	public final <T extends Node> T as(Class<T> type) {
		return support.as(type, mogram, model);
	}

	private Node parent() {
		return support.wrap(model.parentOf(mogram), model);
	}

	@Override
	protected final FrameView frameView() {
		return support.frame(mogram, model, types());
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
