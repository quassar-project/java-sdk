package io.quassar.templating.domain.verification;

public interface Verifier<T> {
	boolean verify(T object) throws VerificationException;
}
