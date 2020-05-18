package com.smaato.switchgear.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;

public class TimeoutSchedulerTest {

    private static final BucketRange TIMEOUT_BUCKET_RANGE = new BucketRange(50, 100);

    private final ScheduledExecutorService schedulerMock = mock(ScheduledExecutorService.class);
    @SuppressWarnings("unchecked")
    private final CompletableFuture<String> futureMock = mock(CompletableFuture.class);

    private final TimeoutScheduler timeoutScheduler = new TimeoutScheduler(schedulerMock);

    @SuppressWarnings("unchecked")
    @Test
    public void whenTimeoutAddedThenCompleteExceptionallyAfterTimeout() {
        when(futureMock.isDone()).thenReturn(false);

        final int timeoutInMillis = 100;

        timeoutScheduler.addTimeout(() -> futureMock, timeoutInMillis, TIMEOUT_BUCKET_RANGE).get();

        final ArgumentCaptor<Runnable> timeoutJobCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(schedulerMock).schedule(timeoutJobCaptor.capture(), eq((long) timeoutInMillis), eq(TimeUnit.MILLISECONDS));

        timeoutJobCaptor.getValue().run();

        final ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(futureMock).completeExceptionally(exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(TimeoutException.class);
    }
}