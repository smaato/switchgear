package com.smaato.switchgear.circuitbreaker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;
import com.smaato.switchgear.circuitbreaker.state.StateManager;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ResultConsumerWithRecognizedExceptionsTest {

    private static final Object VALUE = new Object();

    private final StateManager stateManagerMock = mock(StateManager.class);
    private final LastFailureCause lastFailureCauseMock = mock(LastFailureCause.class);
    private final Exception recognizedException = new RecognizedException();
    private final Set<Class<? extends Exception>> recognizedExceptions = Stream.of(recognizedException.getClass())
                                                                               .collect(Collectors.toSet());

    private final BiConsumer resultConsumer = new ResultConsumer(stateManagerMock, lastFailureCauseMock, recognizedExceptions);

    @Test
    public void whenNoFailureThenHandleSuccess() {
        resultConsumer.accept(VALUE, null);

        verify(stateManagerMock).handleSuccess();
        verifyZeroInteractions(lastFailureCauseMock);
    }

    @Test
    public void whenFailureDetectedThenHandleFailure() {
        final Exception exception = new RecognizedException();

        resultConsumer.accept(VALUE, exception);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(exception);
    }

    @Test
    public void whenFailureSubclassIsDetectedThenHandleFailure() {
        final Exception exception = new RecognizedSubclassException();

        resultConsumer.accept(VALUE, exception);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(exception);
    }

    @Test
    public void whenWrappedFailureIsDetectedThenHandleFailure() {
        final Exception exception = new CompletionException(new RecognizedException());

        resultConsumer.accept(VALUE, exception);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(exception);
    }

    @Test
    public void whenTimeoutIsDetectedThenHandleFailure() {
        final Exception exception = new TimeoutException();

        resultConsumer.accept(VALUE, exception);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(exception);
    }

    @Test
    public void whenWrappedTimeoutIsDetectedThenHandleFailure() {
        final Exception exception = new CompletionException(new TimeoutException());

        resultConsumer.accept(VALUE, exception);

        verify(stateManagerMock).handleFailure();
        verify(lastFailureCauseMock).setFailure(exception);
    }

    private static class RecognizedException extends Exception {
        private static final long serialVersionUID = -1771934166494211052L;
    }

    private static class RecognizedSubclassException extends RecognizedException {
        private static final long serialVersionUID = -5116934763467629201L;
    }
}