package com.smaato.switchgear.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class ExceptionUnwrapperTest {

    private static final TimeoutException CAUSE = new TimeoutException();
    private final ExceptionUnwrapper exceptionUnwrapper = ExceptionUnwrapper.INSTANCE;

    @Test
    public void whenCompletionExceptionThenReturnCause() {
        final CompletionException exception = new CompletionException(CAUSE);

        final Throwable returnValue = exceptionUnwrapper.unwrapAsyncExceptions(exception);

        assertThat(returnValue instanceof TimeoutException, is(true));
    }

    @Test
    public void whenExecutionExceptionThenReturnCause() {
        final ExecutionException exception = new ExecutionException(CAUSE);

        final Throwable returnValue = exceptionUnwrapper.unwrapAsyncExceptions(exception);

        assertThat(returnValue, is(CAUSE));
    }

    @Test
    public void whenSwitchgearRuntimeExceptionThenReturnCause() {
        final SwitchgearRuntimeException exception = new SwitchgearRuntimeException(CAUSE);

        final Throwable returnValue = exceptionUnwrapper.unwrapAsyncExceptions(exception);

        assertThat(returnValue, is(CAUSE));
    }

    @Test
    public void whenCauseExceptionThenReturnId() {
        final Throwable returnValue = exceptionUnwrapper.unwrapAsyncExceptions(CAUSE);

        assertThat(returnValue, is(CAUSE));
    }

    @Test
    public void whenNullThenJustReturn() {
        assertNull(exceptionUnwrapper.unwrapAsyncExceptions(null));
    }
}