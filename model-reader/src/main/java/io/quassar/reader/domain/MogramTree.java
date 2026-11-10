package io.quassar.reader.domain;

import io.quassar.reader.domain.ModelReader.LineReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class MogramTree {
	private final List<MogramBuilder> roots = new ArrayList<>();
	private final Deque<Ancestor> ancestors = new ArrayDeque<>();

	void readLine(ModelTokenizer.Line line, int lineNumber) {
		if (line.tokens().isEmpty()) return;
		MogramBuilder parent = findParent(line.indentation());
		LineReader reader = new LineReader(line.tokens());
		if (reader.isProperty()) {
			readProperty(reader, parent, lineNumber);
			return;
		}
		readMogram(reader, parent, line.indentation());
	}

	Model build() {
		List<Mogram> mograms = new ArrayList<>();
		for (MogramBuilder root : roots) mograms.add(root.build());
		return new Model(mograms);
	}

	private MogramBuilder findParent(int indentation) {
		while (!ancestors.isEmpty() && ancestors.peek().indentation() >= indentation) ancestors.pop();
		return ancestors.isEmpty() ? null : ancestors.peek().mogram();
	}

	private void readProperty(LineReader reader, MogramBuilder parent, int lineNumber) {
		if (parent == null) throw new IllegalArgumentException("Line " + lineNumber + ": root property");
		parent.addProperty(reader.readProperty());
	}

	private void readMogram(LineReader reader, MogramBuilder parent, int indentation) {
		MogramBuilder mogram = reader.readMogram();
		if (parent == null) {
			roots.add(mogram);
		} else parent.addChild(mogram);
		readInlineMembers(reader, mogram);
		ancestors.push(new Ancestor(indentation, mogram));
	}

	private void readInlineMembers(LineReader reader, MogramBuilder mogram) {
		while (reader.take(">")) {
			if (reader.isProperty()) {
				mogram.addProperty(reader.readProperty());
			} else mogram.addChild(reader.readMogram());
		}
	}

	private record Ancestor(int indentation, MogramBuilder mogram) {
	}
}
