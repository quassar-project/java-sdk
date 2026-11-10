package io.quassar.templating.model.outputs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.addAll;

public class Expression implements Output {
	private List<Output> outputs;
	private Expression next = null;

	public Expression(Output... outputs) {
		this.outputs = new ArrayList<>();
		this.output(outputs);
	}

	public Expression output(Output... outputs) {
		addAll(this.outputs, outputs);
		return this;
	}

	public Stream<Output> outputs() {
		return outputs.stream();
	}

	public Expression next() {
		return next;
	}

	public Expression next(Expression or) {
		last().next = or;
		return this;
	}

	private Expression last() {
		return this.next == null ? this : this.next.last();
	}

	public boolean isConstant() {
		return outputs().allMatch(t -> t instanceof Literal);
	}

	@Override
	public String toString() {
		return "<<" + outputs.stream().map(o -> o.toString()).collect(Collectors.joining()) + ">>";
	}
}
