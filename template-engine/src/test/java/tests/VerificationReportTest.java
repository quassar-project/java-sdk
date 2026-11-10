package tests;

import io.quassar.templating.model.verification.VerificationReport;
import io.quassar.templating.model.verification.Issue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VerificationReportTest {
	@Test
	public void reportsSuccessWhenThereAreNoIssues() {
		VerificationReport report = VerificationReport.ok();
		assertThat(report.isOk()).isTrue();
		assertThat(report.getStatus()).isEqualTo(VerificationReport.Status.OK);
		assertThat(report.getIssues()).isEmpty();
	}

	@Test
	public void reportsFailureWhenThereAreIssues() {
		Issue issue = Issue.error(VerificationReport.IssueCode.FINITE_VIOLATION);
		VerificationReport report = VerificationReport.ofIssues(List.of(issue));
		assertThat(report.isOk()).isFalse();
		assertThat(report.getStatus()).isEqualTo(VerificationReport.Status.FAILED);
		assertThat(report.getIssues()).containsExactly(issue);
	}

	@Test
	public void keepsAnImmutableSnapshotOfItsIssues() {
		List<Issue> issues = new ArrayList<>();
		issues.add(Issue.error(VerificationReport.IssueCode.FINITE_VIOLATION));
		VerificationReport report = VerificationReport.ofIssues(issues);
		issues.clear();
		assertThat(report.getIssues()).hasSize(1);
		assertThatThrownBy(() -> report.getIssues().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
