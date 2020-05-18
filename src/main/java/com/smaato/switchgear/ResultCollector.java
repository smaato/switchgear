package com.smaato.switchgear;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Future;
import java.util.function.Function;

import com.smaato.switchgear.concurrent.ExceptionUnwrapper;
import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

class ResultCollector {
    private final ExceptionUnwrapper exceptionUnwrapper;

    ResultCollector(final ExceptionUnwrapper exceptionUnwrapper) {
        this.exceptionUnwrapper = exceptionUnwrapper;
    }

    <T> Outcome<T> getOutcome(final Action<T> action,
                              final Future<T> future,
                              final int timeoutInMillis) {
        try {
            final T executionResult = future.get(timeoutInMillis, MILLISECONDS);
            return new Outcome<>(executionResult);
        } catch (final Exception e) {
            final Throwable unwrapedFailure = exceptionUnwrapper.unwrapAsyncExceptions(e);
            return action.getFailureFallback()
                         .map(f -> getOutcomeFromFallback(f, unwrapedFailure))
                         .orElse(new Outcome<>(unwrapedFailure));
        }
    }

    private static <T> Outcome<T> getOutcomeFromFallback(final Function<Throwable, T> fallback,
                                                         final Throwable failure) {
        final T fallbackResult;
        try {
            fallbackResult = fallback.apply(failure);
        } catch (final RuntimeException e) {
            return new Outcome<>(e);
        }
        return new Outcome<>(fallbackResult);
    }
}
