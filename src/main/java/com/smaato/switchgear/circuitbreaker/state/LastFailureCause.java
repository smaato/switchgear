package com.smaato.switchgear.circuitbreaker.state;

import com.smaato.switchgear.concurrent.ExceptionUnwrapper;

public class LastFailureCause {

    private volatile Throwable last;

    public void setFailure(final Throwable failure) {
        last = ExceptionUnwrapper.INSTANCE.unwrapAsyncExceptions(failure);
    }

    public Throwable get() {
        return last;
    }
}
