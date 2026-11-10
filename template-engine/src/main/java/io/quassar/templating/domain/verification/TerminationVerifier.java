package io.quassar.templating.domain.verification;

import io.quassar.templating.model.Frame;

import java.util.*;

public class TerminationVerifier implements Verifier<Frame> {
	public boolean verify(Frame frame) throws VerificationException {
		return isAcyclic(graphOf(frame));
	}

	private Map<Frame, List<Frame>> graphOf(Frame root) {
		Map<Frame, List<Frame>> graph = new IdentityHashMap<>();
		Deque<Frame> stack = new ArrayDeque<>(List.of(root));
		while (!stack.isEmpty()) {
			Frame current = stack.pop();
			if (graph.containsKey(current)) continue;
			List<Frame> dependencies = dependenciesOf(current);
			graph.put(current, dependencies);
			for (Frame dependency : dependencies)
				if (!graph.containsKey(dependency)) stack.push(dependency);
		}
		return graph;
	}

	private List<Frame> dependenciesOf(Frame frame) {
		return frame.children().stream().filter(child -> child.value() == null).toList();
	}

	private boolean isAcyclic(Map<Frame, List<Frame>> graph) {
		Map<Frame, Integer> indegree = new IdentityHashMap<>();
		graph.keySet().forEach(v -> {
			indegree.putIfAbsent(v, 0);
			graph.get(v).forEach(w -> indegree.merge(w, 1, Integer::sum));
		});
		Deque<Frame> queue = new ArrayDeque<>();
		for (Map.Entry<Frame, Integer> e : indegree.entrySet()) if (e.getValue() == 0) queue.add(e.getKey());

		int visited = 0;
		while (!queue.isEmpty()) {
			Frame v = queue.remove();
			visited++;
			List<Frame> neighbors = graph.getOrDefault(v, List.of());
			neighbors.forEach(w -> {
				int d = indegree.get(w) - 1;
				indegree.put(w, d);
				if (d == 0) queue.add(w);
			});
		}
		return visited == indegree.size();
	}
}
