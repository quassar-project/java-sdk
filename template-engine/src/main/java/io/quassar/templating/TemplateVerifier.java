package io.quassar.templating;

import io.quassar.templating.domain.verification.CompletenessVerifier;
import io.quassar.templating.domain.verification.DeterminacyVerifier;
import io.quassar.templating.domain.verification.ReferenceConsistencyVerifier;
import io.quassar.templating.domain.verification.TerminationVerifier;
import io.quassar.templating.domain.verification.VerificationException;
import io.quassar.templating.domain.verification.Verifier;

import io.quassar.templating.model.Frame;
import io.quassar.templating.model.Template;
import io.quassar.templating.model.verification.Issue;
import io.quassar.templating.model.verification.VerificationReport;

import java.util.ArrayList;
import java.util.List;

import static io.quassar.templating.model.verification.Issue.error;
import static io.quassar.templating.model.verification.VerificationReport.IssueCode.*;

public final class TemplateVerifier {
	private final Template template;

	private TemplateVerifier(Template template) {
		this.template = template;
	}

	public static TemplateVerifier of(Template template) {
		return new TemplateVerifier(template);
	}

	public boolean checkTermination(Frame frame) {
		return check(new TerminationVerifier(), frame);
	}

	public boolean checkDeterminacy() {
		return new DeterminacyVerifier().verify(template);
	}

	public boolean checkCompleteness(Frame frame) {
		return check(new CompletenessVerifier(template), frame);
	}

	public boolean checkReferentialConsistency(Frame frame) {
		return check(new ReferenceConsistencyVerifier(template), frame);
	}

	public VerificationReport verify(Frame frame) {
		if (!checkTermination(frame)) return VerificationReport.ofIssues(List.of(error(FINITE_VIOLATION)));
		List<Issue> issues = new ArrayList<>();
		if (!checkDeterminacy()) issues.add(error(STRATIFICATION_VIOLATION));
		if (!checkCompleteness(frame)) issues.add(error(PARTITION_VIOLATION));
		if (!checkReferentialConsistency(frame)) issues.add(error(REFERENTIAL_CONSISTENCY_VIOLATION));
		return VerificationReport.ofIssues(issues);
	}

	private boolean check(Verifier<Frame> verifier, Frame frame) {
		try {
			return verifier.verify(frame);
		} catch (VerificationException exception) {
			return false;
		}
	}
}
