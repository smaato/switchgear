package com.smaato.switchgear;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.CircuitBreaker;
import com.smaato.switchgear.circuitbreaker.CircuitBreakerHolder;
import com.smaato.switchgear.circuitbreaker.DummyCircuitBreaker;
import com.smaato.switchgear.model.Action;

@SuppressWarnings("unchecked")
public class ExecutorServiceTest {

    private final Executor executorStub = Runnable::run;
    private static final Object SUCCESSFUL_RESULT = new Object();
    private static final Callable<Object> SUCCESSFUL_EXECUTION = () -> SUCCESSFUL_RESULT;
    private static final Callable<Object> FAILING_EXECUTION = () -> {
        throw new Exception();
    };
    private static final String GROUP_NAME = "group name";
    private static final int TIMEOUT_IN_MILLIS = 566;

    private final CircuitBreakerHolder circuitBreakerHolderMock = mock(CircuitBreakerHolder.class);
    private final Function<Throwable, Object> fallbackMock = mock(Function.class);

    private final ExecutorService executorService = new ExecutorService(executorStub, circuitBreakerHolderMock);
    private final CircuitBreaker dummyCircuitBreaker = new DummyCircuitBreaker();

    @Before
    public void setUp() {
        when(circuitBreakerHolderMock.getFor(GROUP_NAME)).thenReturn(dummyCircuitBreaker);
    }

    @Test
    public void whenSuccessfulExecutionThenReturnResult() throws ExecutionException, InterruptedException {
        final Future<Object> actualFuture = executorService.execute(Action.builder(SUCCESSFUL_EXECUTION)
                                                                          .withCircuitBreakerFallback(fallbackMock)
                                                                          .withGroupName(GROUP_NAME)
                                                                          .build(),
                                                                    TIMEOUT_IN_MILLIS);

        assertThat(actualFuture.get(), is(SUCCESSFUL_RESULT));
    }

    @Test
    public void whenFailingExecutionThenReturnWrappedException() {
        final Future<Object> actualFuture = executorService.execute(Action.builder(FAILING_EXECUTION)
                                                                          .withCircuitBreakerFallback(fallbackMock)
                                                                          .withGroupName(GROUP_NAME)
                                                                          .build(),
                                                                    TIMEOUT_IN_MILLIS);

        assertThatThrownBy(actualFuture::get).isInstanceOf(ExecutionException.class);
    }

    @Test
    public void whenRejectedExecutionThenReturnFailingFuture() {
        final Executor executorMock = mock(Executor.class);
        doThrow(RejectedExecutionException.class).when(executorMock).execute(any());

        final ExecutorService localExecutorService = new ExecutorService(executorMock, circuitBreakerHolderMock);

        final Future<Object> actualFuture = localExecutorService.execute(Action.builder(SUCCESSFUL_EXECUTION)
                                                                               .withCircuitBreakerFallback(fallbackMock)
                                                                               .withGroupName(GROUP_NAME)
                                                                               .build(),
                                                                         TIMEOUT_IN_MILLIS);

        assertThatThrownBy(actualFuture::get).hasCause((new RejectedExecutionException()));
    }

    @Test
    public void whenCircuitBreakerThrowExceptionThenReturnFailingFuture() {
        final CircuitBreaker circuitBreakerMock = mock(CircuitBreaker.class);
        when(circuitBreakerMock.execute(any(), any(), eq(TIMEOUT_IN_MILLIS))).thenThrow(new RuntimeException());
        when(circuitBreakerHolderMock.getFor("")).thenReturn(circuitBreakerMock);

        final Future<Object> actualFuture = executorService.execute(Action.builder(SUCCESSFUL_EXECUTION)
                                                                          .withCircuitBreakerFallback(fallbackMock)
                                                                          .withGroupName(GROUP_NAME)
                                                                          .build(),
                                                                    TIMEOUT_IN_MILLIS);

        assertThatThrownBy(actualFuture::get).hasCause(new RuntimeException());
    }
}