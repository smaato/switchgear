package com.smaato.switchgear.circuitbreaker;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;
import com.smaato.switchgear.circuitbreaker.state.StateManager;
import com.smaato.switchgear.concurrent.ExceptionUnwrapper;

class ResultConsumer<T> implements BiConsumer<T, Throwable> {

    private final StateManager stateManager;
    private final LastFailureCause lastFailureCause;
    private final Set<Class<? extends Exception>> recognizedExceptions;

    ResultConsumer(final StateManager stateManager,
                   final LastFailureCause lastFailureCause,
                   final Set<Class<? extends Exception>> recognizedExceptions) {
        this.stateManager = stateManager;
        this.lastFailureCause = lastFailureCause;
        this.recognizedExceptions = recognizedExceptions;
    }

    @Override
    public void accept(final T t,
                       final Throwable failure) {
        if (failure == null) {
            stateManager.handleSuccess();
        } else if (isRecognized(failure)) {
            lastFailureCause.setFailure(failure);
            stateManager.handleFailure();
        }
    }

    private boolean isRecognized(final Throwable failure) {
        if (recognizedExceptions.isEmpty()) {
            return true;
        }

        final Throwable unwrapedFailure = ExceptionUnwrapper.INSTANCE.unwrapAsyncExceptions(failure);

        if (unwrapedFailure instanceof TimeoutException) {
            return true;
        }

        for (final Class<? extends Exception> exception : recognizedExceptions) {
            if (exception.isAssignableFrom(unwrapedFailure.getClass())) {
                return true;
            }
        }

        return false;
    }
}
