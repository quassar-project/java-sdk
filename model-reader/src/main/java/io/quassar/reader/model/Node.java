package io.quassar.reader.model;

import io.quassar.templating.ports.FrameSource;
import io.quassar.templating.ports.FrameView;

import java.util.List;
import java.util.Objects;

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
		return isNull() ? List.of() : model.support().types(mogram);
	}

	public final boolean is(String type) {
		return types().stream().anyMatch(name -> name.equals(type) || name.endsWith("." + type));
	}

	public final boolean is(Class<? extends Node> type) {
		return model.support().is(type, mogram);
	}

	public final <T extends Node> T ancestor(Class<T> type) {
		return model.support().ancestor(type, mogram);
	}

	public final <T extends Node> T as(Class<T> type) {
		return model.support().as(type, mogram);
	}

	protected final boolean hasProperty(String name) {
		return model.support().hasProperty(mogram, name);
	}

	protected final Value property(String name) {
		return model.support().property(mogram, name);
	}

	@Override
	protected final FrameView frameView() {
		return model.support().frame(mogram);
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
