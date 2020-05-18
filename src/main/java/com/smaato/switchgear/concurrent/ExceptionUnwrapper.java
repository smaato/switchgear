package com.smaato.switchgear.concurrent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ExceptionUnwrapper {

    public static final ExceptionUnwrapper INSTANCE = new ExceptionUnwrapper();

    private ExceptionUnwrapper() {
    }

    public Throwable unwrapAsyncExceptions(final Throwable throwable) {
        Throwable failure = throwable;

        if ((failure == null) || (failure.getCause() == null)) {
            return failure;
        }

        while ((failure.getCause() != null) && isWrapperException(failure)) {
            failure = failure.getCause();
            // Recursively unwrap until we get something that is not a wrapper
            failure = unwrapAsyncExceptions(failure);
        }
        return failure;
    }

    private static boolean isWrapperException(final Throwable throwable) {
        return (throwable instanceof CompletionException) ||
                (throwable instanceof ExecutionException) ||
                (throwable instanceof SwitchgearRuntimeException);
    }
}
