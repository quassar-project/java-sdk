package io.quassar.templating.adapters.reflection;

import io.quassar.templating.ports.FrameView;
import io.quassar.templating.ports.FrameView.Getter;
import io.quassar.templating.ports.FrameView.ValueSlot;
import io.quassar.templating.ports.SlotReader;

import java.util.ArrayList;
import java.util.List;

final class ReflectiveSlots implements SlotReader {
	@Override
	public List<Object> read(Object source, FrameView.Slot slot) {
		Object value = slot instanceof Getter getter
			? getter.projection().apply(FrameGetters.read(source, getter.method())) : ((ValueSlot) slot).value();
		List<Object> values = new ArrayList<>();
		append(value, values);
		return List.copyOf(values);
	}

	private void append(Object value, List<Object> values) {
		if (value == null) return;
		if (value instanceof Iterable<?> iterable)
			for (Object item : iterable) append(item, values);
		else if (value.getClass().isArray()) append(ArrayValues.of(value), values);
		else values.add(value);
	}
}
