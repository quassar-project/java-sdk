package io.quassar.templating.model;

import io.quassar.templating.model.predicates.Predicate;
import io.quassar.templating.model.outputs.Placeholder;

import java.util.Iterator;

public class Trigger {
	private final String name;
	private Frame frame;

	public Trigger(String name) {
		this.name = name.toLowerCase();
	}

	public Trigger on(Frame frame) {
		this.frame = frame;
		return this;
	}

	public String name() {
		return name;
	}

	public Frame frame() {
		return frame;
	}

	public Iterator<Frame> frames(String slot) {
		return frame.frames(slot.toLowerCase());
	}

	public Iterator<Frame> frames(Placeholder placeholder) {
		Frame frame = resolve(this.frame, placeholder.targetPath());
		return frame.frames(placeholder.name().toLowerCase());
	}

	boolean check(Predicate condition) {
		return condition.evaluate(this);
	}

	public static Frame resolve(Frame frame, String[] path) {
		if (path == null || path.length == 0) return frame;
		Frame current = frame;
		for (String step : path)
			if (step.equalsIgnoreCase("container")) current = current.container();
			else if (step.equalsIgnoreCase("root")) current = root(current);
			else {
				Iterator<Frame> frames = current.frames(step.toLowerCase());
				if (frames.hasNext()) current = frames.next();
				else return frame;
			}
		return current;
	}

	private static Frame root(Frame frame) {
		Frame current = frame;
		while (current != null) {
			if (current.container() == null) return current;
			current = current.container();
		}
		return current;
	}

}
