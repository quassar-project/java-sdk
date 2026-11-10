package io.quassar.templating.domain.verification;

import io.quassar.templating.model.Template;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.predicates.AttributePredicate;
import io.quassar.templating.model.predicates.TriggerPredicate;
import io.quassar.templating.model.predicates.TypePredicate;
import io.quassar.templating.model.predicates.LogicalExpression;
import io.quassar.templating.model.predicates.BinaryExpression;
import io.quassar.templating.model.predicates.BinaryOperator;
import io.quassar.templating.model.predicates.NotExpression;

import java.util.*;

import static io.quassar.templating.domain.verification.DeterminacyVerifier.Atom.Kind.ATTRIBUTE;

public class DeterminacyVerifier implements Verifier<Template> {

	public boolean verify(Template template) {
		List<Rule> rules = template.ruleSet();
		int n = rules.size();
		if (n <= 1) return true;
		for (int i = 0; i < n; i++) {
			Rule ri = rules.get(i);
			for (int j = i + 1; j < n; j++) {
				Rule rj = rules.get(j);
				if (!areMutuallyExclusive(ri.condition(), rj.condition())) return false;
			}
		}
		return true;
	}

	private boolean areMutuallyExclusive(LogicalExpression c1, LogicalExpression c2) {
		LogicalExpression nnf1 = toNNF(c1, false);
		LogicalExpression nnf2 = toNNF(c2, false);
		List<Set<Atom>> dnf1 = toDNF(nnf1);
		List<Set<Atom>> dnf2 = toDNF(nnf2);
		for (Set<Atom> conj1 : dnf1)
			if (dnf2.stream().anyMatch(conj2 -> isConjunctionSatisfiable(conj1, conj2))) return false;
		return true;
	}

	private boolean isConjunctionSatisfiable(Set<Atom> c1, Set<Atom> c2) {
		List<Atom> all = new ArrayList<>(c1.size() + c2.size());
		all.addAll(c1);
		all.addAll(c2);
		for (int i = 0; i < all.size(); i++) {
			Atom li = all.get(i);
			for (int j = i + 1; j < all.size(); j++) {
				Atom lj = all.get(j);
				if (li.kind == lj.kind &&
						Objects.equals(li.name, lj.name) &&
						Objects.equals(li.value, lj.value) &&
						li.positive != lj.positive) {
					return false; // contradictorio
				}
			}
		}
		Map<String, Object> attrValues = new HashMap<>();
		for (Atom l : all)
			if (l.kind == ATTRIBUTE && l.positive) {
				Object prev = attrValues.putIfAbsent(l.name, l.value);
				if (prev != null && !Objects.equals(prev, l.value)) return false;
			}
		String trigger = null;
		for (Atom l : all)
			if (l.kind == Atom.Kind.TRIGGER && l.positive) {
				if (trigger == null) trigger = l.name;
				else if (!trigger.equals(l.name)) return false;
			}
		return true;
	}

	private LogicalExpression toNNF(LogicalExpression expr, boolean negated) {
		if (expr instanceof NotExpression n)
			return toNNF(n.expression(), !negated);

		if (expr instanceof BinaryExpression b) {
			LogicalExpression left = toNNF(b.left(), negated);
			LogicalExpression right = toNNF(b.right(), negated);
			if (!negated) {
				return new BinaryExpression(left, b.operator(), right);
			} else {
				BinaryOperator op = (b.operator() == BinaryOperator.AND)
						? BinaryOperator.OR
						: BinaryOperator.AND;
				return new BinaryExpression(left, op, right);
			}
		}

		if (negated) return new NotExpression(expr);
		return expr;
	}

	private List<Set<Atom>> toDNF(LogicalExpression exprNNF) {
		if (exprNNF instanceof BinaryExpression b) {
			List<Set<Atom>> left = toDNF(b.left());
			List<Set<Atom>> right = toDNF(b.right());
			List<Set<Atom>> result;

			if (b.operator() == BinaryOperator.AND) {
				result = new ArrayList<>();
				for (Set<Atom> c1 : left)
					for (Set<Atom> c2 : right) {
						Set<Atom> conj = new HashSet<>(c1);
						conj.addAll(c2);
						result.add(conj);
					}
			} else {
				result = new ArrayList<>(left.size() + right.size());
				result.addAll(left);
				result.addAll(right);
			}
			return result;
		}

		if (exprNNF instanceof NotExpression n) {
			Atom inner = toAtom(n.expression(), true);      // átomo positivo
			Atom neg = new Atom(inner.kind, inner.name, inner.value, false); // versión negativa
			return List.of(Set.of(neg));
		}
		return List.of(Set.of(toAtom(exprNNF, true)));
	}

	private Atom toAtom(LogicalExpression atom, boolean positive) {
		if (atom instanceof TriggerPredicate t)
			return new Atom(Atom.Kind.TRIGGER, t.name(), null, positive);
		if (atom instanceof AttributePredicate a)
			return new Atom(ATTRIBUTE, a.attribute(), a.value(), positive);
		if (atom instanceof TypePredicate tp)
			return new Atom(Atom.Kind.TYPE, tp.types()[0], null, positive);
		throw new IllegalArgumentException("Unexpected atom type: " + atom.getClass());
	}

	public static final class Atom {
		enum Kind {TYPE, TRIGGER, ATTRIBUTE}

		final Kind kind;
		final String name;
		final Object value;
		final boolean positive;

		public Atom(Kind kind, String name, Object value, boolean positive) {
			this.kind = kind;
			this.name = name;
			this.value = value;
			this.positive = positive;
		}

		Atom negated() {
			return new Atom(kind, name, value, !positive);
		}

		@Override
		public String toString() {
			return (positive ? "" : "not ") + kind + "(" + name + (kind == ATTRIBUTE ? "=" + value : "") + ")";
		}
	}
}
