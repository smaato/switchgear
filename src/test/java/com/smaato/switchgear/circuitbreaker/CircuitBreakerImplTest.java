package com.smaato.switchgear.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.state.ConsecutiveFailuresStateManager;
import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRangeFinder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedFailureStatesHolder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedStateManagersHolder;

@SuppressWarnings({"unchecked", "TypeMayBeWeakened", "ClassWithTooManyFields"})
public class CircuitBreakerImplTest {

    private static final String FALLBACK_RESULT = "fallback";
    private static final int TIMEOUT = 60;
    private static final BucketRange TIMEOUT_BUCKET_RANGE = new BucketRange(50, 100);

    private final ConsecutiveFailuresStateManager stateManagerMock = mock(ConsecutiveFailuresStateManager.class);
    private final TimeoutScheduler timeoutSchedulerMock = mock(TimeoutScheduler.class);
    private final LastFailureCause lastFailureCauseMock = mock(LastFailureCause.class);
    private final BucketRangeFinder bucketRangeFinderMock = mock(BucketRangeFinder.class);
    private final BucketedStateManagersHolder bucketedStateManagersHolderMock = mock(BucketedStateManagersHolder.class);
    private final BucketedFailureStatesHolder bucketedFailureStatesHolderMock = mock(BucketedFailureStatesHolder.class);

    private final CompletableFuture<String> futureMock = mock(CompletableFuture.class);
    private final CompletableFuture<String> enhancedFuture = mock(CompletableFuture.class);
    private final CompletableFuture<String> timeoutFuture = mock(CompletableFuture.class);
    private final Supplier<CompletableFuture<String>> futureSupplier = () -> futureMock;
    private final Function<Throwable, String> fallbackFunctionMock = mock(Function.class);
    private final Set<Class<? extends Exception>> recognizedExceptionsMock = mock(Set.class);

    private final CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(bucketedStateManagersHolderMock,
                                                                             timeoutSchedulerMock,
                                                                             bucketedFailureStatesHolderMock,
                                                                             bucketRangeFinderMock,
                                                                             recognizedExceptionsMock);
    private final Throwable lastFailErrorMock = mock(Throwable.class);

    @Before
    public void before() {
        when(bucketRangeFinderMock.find(TIMEOUT)).thenReturn(TIMEOUT_BUCKET_RANGE);
        when(bucketedFailureStatesHolderMock.getFailureState(TIMEOUT_BUCKET_RANGE)).thenReturn(lastFailureCauseMock);
        when(lastFailureCauseMock.get()).thenReturn(lastFailErrorMock);

        when(bucketedStateManagersHolderMock.getStateManager(TIMEOUT_BUCKET_RANGE)).thenReturn(stateManagerMock);

        when(fallbackFunctionMock.apply(any())).thenReturn(FALLBACK_RESULT);
        when(timeoutSchedulerMock.addTimeout(futureSupplier, TIMEOUT, TIMEOUT_BUCKET_RANGE)).thenReturn(() -> timeoutFuture);
        when(timeoutFuture.whenComplete(any())).thenReturn(enhancedFuture);
    }

    @Test
    public void whenCircuitIsClosedThenReturnRealAction() throws Exception {
        when(stateManagerMock.isOpen()).thenReturn(false);
        final Future<String> actualFuture = circuitBreaker.execute(futureSupplier, fallbackFunctionMock, TIMEOUT);

        assertThat(actualFuture).isEqualTo(enhancedFuture);

        actualFuture.get();
        verifyZeroInteractions(fallbackFunctionMock);
    }

    @Test
    public void whenCircuitIsOpenThenReturnFallback() throws Exception {
        when(stateManagerMock.isOpen()).thenReturn(true);

        final Future<String> actualFuture = circuitBreaker.execute(futureSupplier, fallbackFunctionMock, TIMEOUT);

        assertThat(actualFuture.get()).isEqualTo(FALLBACK_RESULT);

        verify(fallbackFunctionMock).apply(lastFailErrorMock);
    }
}