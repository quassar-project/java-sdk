package io.quassar.reader.domain;

import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

final class MogramIdentity {
	private final MessageDigest digest;

	private MogramIdentity() {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	static String fingerprint(String type, List<Property> properties, List<String> facets, List<Child> children) {
		MogramIdentity identity = new MogramIdentity();
		identity.text("mogram:1");
		identity.text(type);
		identity.integer(facets.size());
		facets.forEach(identity::text);
		identity.integer(properties.size());
		properties.stream().sorted(Comparator.comparing(Property::name, Comparator.nullsFirst(Comparator.naturalOrder())))
			.forEach(identity::property);
		identity.integer(children.size());
		for (Child child : children) {
			identity.text(child.identifier());
			identity.text(child.fingerprint());
		}
		return HexFormat.of().formatHex(identity.digest.digest());
	}

	static String context(String parent, Declaration declaration, int occurrence) {
		MogramIdentity identity = new MogramIdentity();
		identity.text("context:1");
		identity.text(parent);
		identity.text(declaration.type());
		identity.text(declaration.identifier());
		identity.text(declaration.fingerprint());
		identity.integer(occurrence);
		return HexFormat.of().formatHex(identity.digest.digest());
	}

	private void property(Property property) {
		boolean positional = property.name() == null;
		text(positional ? "positional" : "named");
		if (positional) {
			text(property.constructorType());
			integer(property.position());
		} else text(property.name());
		value(property.value());
	}

	private void value(Value value) {
		if (value instanceof Value.Text text) scalar("string", text.value());
		else if (value instanceof Value.Number number) scalar("number", number.value().stripTrailingZeros().toPlainString());
		else if (value instanceof Value.Bool bool) scalar("boolean", Boolean.toString(bool.value()));
		else if (value instanceof Value.Reference reference) scalar("reference", reference.path());
		else if (value instanceof Value.Expression expression) scalar("expression", expression.code());
		else if (value instanceof Value.Sequence sequence) {
			text("sequence");
			integer(sequence.values().size());
			sequence.values().forEach(this::value);
		} else text("empty");
	}

	private void scalar(String type, String value) {
		text(type);
		text(value);
	}

	private void text(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		integer(bytes.length);
		digest.update(bytes);
	}

	private void integer(int value) {
		digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
	}

	record Declaration(String type, String identifier, String fingerprint) {
	}

	record Child(String identifier, String fingerprint) {
	}
}
