package com.smaato.switchgear;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.junit.Test;

import com.smaato.switchgear.concurrent.ExceptionUnwrapper;
import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

@SuppressWarnings("unchecked")
public class ResultCollectorTest {

    private static final Object SUCCESSFUL_RESULT = new Object();
    private static final Object FALLBACK_RESULT = new Object();
    private static final int TIMEOUT_IN_MILLIS = 100;
    private static final Action<Object> DEFAULT_ACTION = Action.builder(() -> null)
                                                               .withCircuitBreakerFallback(ex -> FALLBACK_RESULT)
                                                               .build();

    private final Future<Object> futureMock = mock(Future.class);
    private final ExceptionUnwrapper exceptionUnwrapperMock = mock(ExceptionUnwrapper.class);
    private final Throwable wrappingFailure = new RuntimeException();
    private final Throwable causeFailure = new Exception();

    private final ResultCollector resultCollector = new ResultCollector(exceptionUnwrapperMock);

    @Test
    public void whenGettingOutcomeFromSuccessfulExecutionThenReturnResult() throws Exception {
        when(futureMock.get(TIMEOUT_IN_MILLIS, MILLISECONDS)).thenReturn(SUCCESSFUL_RESULT);

        final Outcome<Object> outcome = resultCollector.getOutcome(DEFAULT_ACTION, futureMock, TIMEOUT_IN_MILLIS);

        assertTrue(outcome.getValue().isPresent());
        assertThat(outcome.getValue().get(), is(SUCCESSFUL_RESULT));
        assertFalse(outcome.getFailure().isPresent());
    }

    @Test
    public void whenGettingOutcomeFromFailedExecutionThenReturnUnwrappedFailure() throws Exception {
        when(exceptionUnwrapperMock.unwrapAsyncExceptions(wrappingFailure)).thenReturn(causeFailure);

        when(futureMock.get(TIMEOUT_IN_MILLIS, MILLISECONDS)).thenThrow(wrappingFailure);

        final Outcome<Object> outcome = resultCollector.getOutcome(DEFAULT_ACTION, futureMock, TIMEOUT_IN_MILLIS);

        assertFalse(outcome.getValue().isPresent());
        assertTrue(outcome.getFailure().isPresent());
        assertThat(outcome.getFailure().get(), is(causeFailure));
    }

    @Test
    public void whenGettingNoResultThenResultNotPresentInTheOutcome() throws Exception {
        when(futureMock.get(TIMEOUT_IN_MILLIS, MILLISECONDS)).thenReturn(null);

        final Outcome<Object> outcome = resultCollector.getOutcome(DEFAULT_ACTION, futureMock, TIMEOUT_IN_MILLIS);

        assertFalse(outcome.getValue().isPresent());
        assertFalse(outcome.getFailure().isPresent());
    }

    @Test
    public void whenFailedExecutionAndFallbackEnabledForAllThenReturnFallbackResult() throws Exception {
        final Action<Object> fallbackAction = Action.builder(() -> null)
                                                    .withFailureFallback(ex -> FALLBACK_RESULT).build();
        when(exceptionUnwrapperMock.unwrapAsyncExceptions(wrappingFailure)).thenReturn(causeFailure);
        when(futureMock.get(TIMEOUT_IN_MILLIS, MILLISECONDS)).thenThrow(wrappingFailure);

        final Outcome<Object> outcome = resultCollector.getOutcome(fallbackAction, futureMock, TIMEOUT_IN_MILLIS);

        assertFalse(outcome.getFailure().isPresent());
        assertTrue(outcome.getValue().isPresent());
        assertThat(outcome.getValue().get(), is(FALLBACK_RESULT));
    }

    @Test
    public void whenFailedExecutionAndFallbackThrowsExceptionThenReturnFallbackException() throws Exception {
        final RuntimeException fallbackException = new RuntimeException();
        final Action<Object> failingFallbackAction = Action.builder(() -> null)
                                                           .withFailureFallback(ex -> {
                                                               throw fallbackException;
                                                           }).build();
        when(exceptionUnwrapperMock.unwrapAsyncExceptions(wrappingFailure)).thenReturn(causeFailure);
        when(futureMock.get(TIMEOUT_IN_MILLIS, MILLISECONDS)).thenThrow(wrappingFailure);

        final Outcome<Object> outcome = resultCollector.getOutcome(failingFallbackAction, futureMock, TIMEOUT_IN_MILLIS);

        assertFalse(outcome.getValue().isPresent());
        assertTrue(outcome.getFailure().isPresent());
        assertThat(outcome.getFailure().get(), is(fallbackException));
    }
}