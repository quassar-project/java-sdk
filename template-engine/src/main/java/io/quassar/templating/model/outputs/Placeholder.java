package io.quassar.templating.model.outputs;

public class Placeholder implements Output {
	private static final String OptionSeparator = "+";
	public static final String THIS = "this";
	public static final Output This = new Placeholder("");

	private final boolean isThis;
	private final String name;
	private final String[] formatters;
	private final String[] targetPath;

	private String separator = null;

	public Placeholder(String name, String... formatters) {
		this(name, null, formatters);
	}

	public Placeholder(String name, String[] targetPath, String... formatters) {
		this.isThis = name.equals(THIS) || name.isEmpty();
		this.name = this.isThis ? "" : name;
		this.targetPath = targetPath;
		this.formatters = formatters;
	}

	public String name() {
		return name;
	}

	public String[] targetPath() {
		return targetPath;
	}

	public String[] formatters() {
		return formatters;
	}

	public String separator() {
		return separator;
	}

	public boolean isThis() {
		return isThis;
	}

	private boolean isMultiple() {
		return separator != null;
	}

	public String fullName() {
		StringBuilder result = new StringBuilder(name);
		if (targetPath != null) result.append("<").append(String.join(".", targetPath)).append(">");
		for (String formatter : formatters) result.append(OptionSeparator).append(formatter);
		return result.toString();
	}

	public Placeholder multiple(String separator) {
		this.separator = separator;
		return this;
	}

	@Override
	public String toString() {
		return "$" + fullName() + (isMultiple() ? "...[" + (separator != null ? separator : ":") + "]" : "");
	}
}
