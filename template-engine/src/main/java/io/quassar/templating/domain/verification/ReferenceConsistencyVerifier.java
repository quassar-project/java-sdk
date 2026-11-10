package io.quassar.templating.domain.verification;

import io.quassar.templating.model.Template;

public final class ReferenceConsistencyVerifier extends FrameVerifier {
	public ReferenceConsistencyVerifier(Template template) {
		super(template, formatter -> true);
	}
}
