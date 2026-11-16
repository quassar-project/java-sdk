package io.quassar.templating.adapters.antlr;

import org.antlr.v4.runtime.*;

final class TemplateErrorStrategy extends DefaultErrorStrategy {

	@Override
	public void reportError(Parser recognizer, RecognitionException e) {
		throw new RecognitionException(recognizer, recognizer.getInputStream(), recognizer.getContext());
	}

	@Override
	protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
		super.reportNoViableAlternative(recognizer, e);
		throw new RecognitionException(getMessage(recognizer), recognizer, recognizer.getInputStream(), recognizer.getContext());
	}

	@Override
	protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
		super.reportInputMismatch(recognizer, e);
		throw new RecognitionException(getMessage(recognizer), recognizer, recognizer.getInputStream(), recognizer.getContext());
	}

	@Override
	protected void reportFailedPredicate(Parser recognizer, FailedPredicateException e) {
		super.reportFailedPredicate(recognizer, e);
		throw new RecognitionException(getMessage(recognizer), recognizer, recognizer.getInputStream(), recognizer.getContext());
	}

	@Override
	protected void reportUnwantedToken(Parser recognizer) {
		super.reportUnwantedToken(recognizer);
		throw new RecognitionException(getMessage(recognizer), recognizer, recognizer.getInputStream(), recognizer.getContext());

	}

	private String getMessage(Parser recognizer) {
		return "Unwanted Token; expected " + recognizer.getExpectedTokens().toString(recognizer.getVocabulary());
	}

	@Override
	protected void reportMissingToken(Parser recognizer) {
		super.reportMissingToken(recognizer);
	}
}
