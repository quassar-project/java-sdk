package tests;

import io.quassar.templating.TemplateVerifier;

import io.quassar.templating.domain.verification.TerminationVerifier;

import tests.TestTemplates;
import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import io.quassar.templating.model.Frame;
import io.quassar.templating.Frames;
import io.quassar.templating.model.verification.VerificationReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static tests.fixtures.LibraryFixtures.book;
import static io.quassar.templating.model.verification.VerificationReport.IssueCode.*;

public class TemplateVerifierTest {
	@Test
	public void detectsCyclesInFrames() {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book");
		ReflectiveFrameBuilder chapter = new ReflectiveFrameBuilder("Chapter");
		Frame frame = chapter.toFrame();
		chapter.add("book", book);
		book.add("chapter", frame);
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/book-summary")).checkTermination(book.toFrame())).isFalse();
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/book-summary")).checkTermination(
			new ReflectiveFrameBuilder("Book").add("title", "Don Quixote").toFrame())).isTrue();
	}

	@Test
	public void returnsFalseForACycleWithoutHashingTheFrameGraph() throws Exception {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book");
		ReflectiveFrameBuilder chapter = new ReflectiveFrameBuilder("Chapter");
		Frame frame = chapter.toFrame();
		chapter.add("book", book);
		book.add("chapter", frame);
		assertThat(new TerminationVerifier().verify(book.toFrame())).isFalse();
	}

	@Test
	public void acceptsMutuallyExclusiveConditions() {
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/exclusive-genres")).checkDeterminacy()).isTrue();
	}

	@Test
	public void detectsOverlappingRules() {
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/overlapping-rules")).checkDeterminacy()).isFalse();
	}

	@Test
	public void acceptsFramesWithMatchingRulesAndFormatters() {
		Frame frame = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote").toFrame();
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/formatted-title")).checkCompleteness(frame)).isTrue();
	}

	@Test
	public void rejectsFramesWithoutAMatchingRule() {
		Frame frame = new ReflectiveFrameBuilder("Library").add("name", "City Library").toFrame();
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/book-summary")).checkCompleteness(frame)).isFalse();
	}

	@Test
	public void acceptsReferencesToExistingSlots() {
		Frame frame = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote").toFrame();
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/book-summary")).checkReferentialConsistency(frame)).isTrue();
	}

	@Test
	public void rejectsReferencesToMissingSlots() {
		Frame frame = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote").toFrame();
		assertThat(TemplateVerifier.of(TestTemplates.read("verification/book-isbn")).checkReferentialConsistency(frame)).isFalse();
	}

	@Test
	public void reportsSuccessForACompleteTemplateAndFrame() {
		var verifier = TemplateVerifier.of(TestTemplates.read("verification/book-summary"));
		VerificationReport report = verifier.verify(Frames.from(book()));
		assertThat(report.isOk()).isTrue();
		assertThat(report.getIssues()).isEmpty();
	}

	@Test
	public void reportsBothCompletenessAndReferenceIssues() {
		var verifier = TemplateVerifier.of(TestTemplates.read("verification/book-isbn"));
		VerificationReport report = verifier.verify(Frames.from(book()));
		assertThat(report.isOk()).isFalse();
		assertThat(report.getIssues()).extracting(issue -> issue.code())
			.containsExactly(PARTITION_VIOLATION, REFERENTIAL_CONSISTENCY_VIOLATION);
	}

	@Test
	public void reportsACycleBeforeRecursivelyCheckingSlots() {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book");
		ReflectiveFrameBuilder chapter = new ReflectiveFrameBuilder("Chapter");
		Frame chapterFrame = chapter.toFrame();
		chapter.add("next", book);
		book.add("next", chapterFrame);
		var verifier = TemplateVerifier.of(TestTemplates.read("verification/frame-cycle"));
		VerificationReport report = verifier.verify(book.toFrame());
		assertThat(report.isOk()).isFalse();
		assertThat(report.getIssues()).extracting(issue -> issue.code()).containsExactly(FINITE_VIOLATION);
	}
}
