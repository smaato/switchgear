package com.smaato.switchgear.circuitbreaker.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class LastFailureCauseTest {

    private final LastFailureCause lastFailureCause = new LastFailureCause();

    @Test
    public void whenCompletionExceptionThenStoreWrappedException() {
        final Exception original = new RuntimeException();
        final Throwable ex = new CompletionException(original);

        lastFailureCause.setFailure(ex);

        assertThat(lastFailureCause.get()).isEqualTo(original);
    }

    @Test
    public void whenExecutionExceptionThenStoreWrappedException() {
        final Exception original = new RuntimeException();
        final Throwable ex = new ExecutionException(original);

        lastFailureCause.setFailure(ex);

        assertThat(lastFailureCause.get()).isEqualTo(original);
    }

    @Test
    public void whenNotExecutionOrCompletionExceptionThenStoreSameException() {
        final Exception innerException = new RuntimeException();
        final Exception originalException = new RuntimeException(innerException);

        lastFailureCause.setFailure(originalException);

        assertThat(lastFailureCause.get()).isEqualTo(originalException);
    }
}