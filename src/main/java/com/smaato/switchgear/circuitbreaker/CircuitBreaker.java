package com.smaato.switchgear.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CircuitBreaker {

    <T> CompletableFuture<T> execute(final Supplier<CompletableFuture<T>> futureSupplier,
                                     final Function<Throwable, T> fallback,
                                     int timeoutInMillis);
}
