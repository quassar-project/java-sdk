package io.quassar.reader.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class Resource {
	private final String name;
	private final byte[] bytes;

	Resource(String name, byte[] bytes) {
		this.name = name;
		this.bytes = bytes.clone();
	}

	public String name() {
		return name;
	}

	public byte[] bytes() {
		return bytes.clone();
	}

	public InputStream open() {
		return new ByteArrayInputStream(bytes);
	}
}
