package com.smaato.switchgear.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CompletedFuture<T> extends CompletableFuture<T> {

    private final Supplier<T> action;

    public CompletedFuture(final Supplier<T> action) {
        this.action = action;
    }

    @Override
    public T getNow(final T valueIfAbsent) {
        return action.get();
    }

    @Override
    public T get() {
        return action.get();
    }

    @Override
    public T get(final long timeout,
                 final TimeUnit unit) {
        return action.get();
    }
}
