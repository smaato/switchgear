package com.smaato.switchgear.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;

class TimeoutScheduler {

    private static final boolean DO_NOT_INTERRUPT = false;

    private final ScheduledExecutorService scheduler;

    TimeoutScheduler(final ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    <T> Supplier<CompletableFuture<T>> addTimeout(final Supplier<CompletableFuture<T>> futureSupplier,
                                                  final int timeoutInMillis,
                                                  final BucketRange timeoutBucketRange) {
        return () -> scheduleTimeout(futureSupplier.get(), timeoutInMillis, timeoutBucketRange);
    }

    private <T> CompletableFuture<T> scheduleTimeout(final CompletableFuture<T> future,
                                                     final int timeoutInMillis,
                                                     final BucketRange timeoutBucketRange) {
        final ScheduledFuture<?> timeoutCheckFuture = scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(TimeoutExceptionBuilder.builder().withTimeoutRange(timeoutBucketRange).build());
            }
        }, timeoutInMillis, TimeUnit.MILLISECONDS);

        // Configure the event future to delete the timeout check immediately when it finishes.
        future.whenComplete((event, error) -> {
            if (!timeoutCheckFuture.isDone()) {
                timeoutCheckFuture.cancel(DO_NOT_INTERRUPT);
            }
        });

        return future;
    }
}
