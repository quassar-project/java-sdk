package io.quassar.templating.domain.verification;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.Trigger;
import io.quassar.templating.model.Template;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.outputs.Output;
import io.quassar.templating.model.outputs.Expression;
import io.quassar.templating.model.outputs.Placeholder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class FrameVerifier implements Verifier<Frame> {
	private final Template template;
	private final Predicate<String> supportsFormatter;

	protected FrameVerifier(Template template, Predicate<String> supportsFormatter) {
		this.template = template;
		this.supportsFormatter = supportsFormatter;
	}

	@Override
	public boolean verify(Frame frame) throws VerificationException {
		evaluate(new Trigger("root").on(frame));
		return true;
	}

	private void evaluate(Trigger trigger) throws VerificationException {
		Rule rule = findRuleFor(trigger).orElseThrow(() -> new VerificationException("Rule not found for trigger " + trigger.name()));
		Frame frame = trigger.frame();
		evaluate(rule, frame);
	}

	private void evaluate(Rule rule, Frame frame) throws VerificationException {
		List<Placeholder> placeholders = placeholders(rule);
		for (Placeholder p : placeholders) {
			if (!p.isThis() && !frame.contains(p.name()))
				throw new VerificationException("Frame for placeholder " + p.name() + " not found");
			Iterator<Frame> frames = p.isThis() ? List.of(frame).iterator() : frame.frames(p.name());
			while (frames.hasNext()) {
				Frame next = frames.next();
				for (String formatter : p.formatters()) {
					Optional<Rule> ruleFor = findRuleFor(new Trigger(formatter).on(next));
					if (!ruleFor.isPresent() && !supportsFormatter.test(formatter))
						throw new VerificationException("Formatter " + formatter + " not found");
					else if (ruleFor.isPresent()) evaluate(ruleFor.get(), next);
				}
			}
		}
	}

	private Optional<Rule> findRuleFor(Trigger trigger) {
		return template.ruleSet().stream()
				.filter(r -> r.condition().evaluate(trigger))
				.findFirst();
	}

	private static List<Placeholder> placeholders(Rule rule) {
		List<Output> outputs = rule.outputs().toList();
		List<Placeholder> placeholders = placeholders(outputs);
		placeholders.addAll(outputs.stream()
				.filter(o -> o instanceof Expression)
				.map(o -> placeholders(((Expression) o).outputs().toList()))
				.flatMap(Collection::stream)
				.toList());
		return placeholders;
	}

	private static List<Placeholder> placeholders(List<Output> outputs) {
		return outputs.stream()
				.filter(o -> o instanceof Placeholder)
				.map(p -> (Placeholder) p)
				.collect(Collectors.toList());
	}

}
