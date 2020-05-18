package com.smaato.switchgear.circuitbreaker;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DummyCircuitBreakerTest {

    private static final int TIMEOUT_IN_MILLIS = 100;
    private final CircuitBreaker circuitBreaker = new DummyCircuitBreaker();
    private final CompletableFuture<Object> expectedFutureMock = mock(CompletableFuture.class);
    private final Supplier<CompletableFuture<Object>> completableFutureSupplier = () -> expectedFutureMock;
    private final Function fallbackMock = mock(Function.class);

    @Test
    public void givenFutureSupplierThenReturnFuture() {
        final CompletableFuture future = circuitBreaker.execute(completableFutureSupplier, fallbackMock, TIMEOUT_IN_MILLIS);

        assertThat(future, is(expectedFutureMock));

        verifyZeroInteractions(fallbackMock);
    }
}