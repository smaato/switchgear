package com.smaato.switchgear.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class DummyCircuitBreaker implements CircuitBreaker {
    @Override
    public <R> CompletableFuture<R> execute(final Supplier<CompletableFuture<R>> completableFutureSupplier,
                                            final Function<Throwable, R> fallback,
                                            final int timeoutInMillis) {
        return completableFutureSupplier.get();
    }
}
