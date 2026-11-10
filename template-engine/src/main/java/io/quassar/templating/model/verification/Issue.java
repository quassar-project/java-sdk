package io.quassar.templating.model.verification;

public record Issue(VerificationReport.IssueCode code) {
	public static Issue error(VerificationReport.IssueCode code) {
		return new Issue(code);
	}
}
