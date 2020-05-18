package com.smaato.switchgear;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import com.smaato.switchgear.circuitbreaker.CircuitBreakerHolder;
import com.smaato.switchgear.concurrent.Utils;
import com.smaato.switchgear.model.Action;

class ExecutorService {

    private final Executor executor;
    private final CircuitBreakerHolder circuitBreakerHolder;

    ExecutorService(final Executor executor,
                    final CircuitBreakerHolder circuitBreakerHolder) {
        this.executor = executor;
        this.circuitBreakerHolder = circuitBreakerHolder;
    }

    <T> Future<T> execute(final Action<T> action,
                          final int timeoutInMillis) {

        final Supplier<CompletableFuture<T>> deferredExecution = wrapToDeferredExecution(action.getCall());

        return getCompletableFutureSafely(() -> circuitBreakerHolder.getFor(action.getGroupName())
                                                                    .execute(deferredExecution,
                                                                             action.getCircuitBreakerFallback(),
                                                                             timeoutInMillis));
    }

    private <T> Supplier<CompletableFuture<T>> wrapToDeferredExecution(final Callable<T> callable) {
        return () -> getCompletableFutureSafely(() -> CompletableFuture.supplyAsync(Utils.singleton().convertToSupplier(callable),
                                                                                    executor));
    }

    private static <T> CompletableFuture<T> getCompletableFutureSafely(final Supplier<CompletableFuture<T>> completableFutureSupplier) {
        try {
            return completableFutureSupplier.get();
        } catch (final Exception exception) {
            final CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(exception);
            return exceptionFuture;
        }
    }
}
