package com.smaato.switchgear.circuitbreaker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;
import com.smaato.switchgear.circuitbreaker.state.StateManager;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ResultConsumerTest {

    private static final Object VALUE = new Object();

    private final Throwable failureMock = mock(Throwable.class);

    private final StateManager stateManagerMock = mock(StateManager.class);
    private final LastFailureCause lastFailureCauseMock = mock(LastFailureCause.class);
    private final Set<Class<? extends Exception>> whiteListedExceptions = new HashSet<>();

    private final BiConsumer resultConsumer = new ResultConsumer(stateManagerMock, lastFailureCauseMock, whiteListedExceptions);

    @Test
    public void whenNoFailureThenHandleSuccess() {
        resultConsumer.accept(VALUE, null);

        verify(stateManagerMock).handleSuccess();
        verifyZeroInteractions(lastFailureCauseMock);
    }

    @Test
    public void whenFailureDetectedThenHandleFailure() {
        resultConsumer.accept(VALUE, failureMock);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(failureMock);
    }
}