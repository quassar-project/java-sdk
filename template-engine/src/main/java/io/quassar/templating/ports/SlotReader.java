package io.quassar.templating.ports;

import java.util.List;

@FunctionalInterface
public interface SlotReader {
	List<Object> read(Object source, FrameView.Slot slot);
}
