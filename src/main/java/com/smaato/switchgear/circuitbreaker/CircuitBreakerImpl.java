package com.smaato.switchgear.circuitbreaker;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;
import com.smaato.switchgear.circuitbreaker.state.StateManager;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRangeFinder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedFailureStatesHolder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedStateManagersHolder;
import com.smaato.switchgear.concurrent.CompletedFuture;

class CircuitBreakerImpl implements CircuitBreaker {

    private final BucketedStateManagersHolder stateManagersHolder;
    private final TimeoutScheduler timeoutScheduler;
    private final BucketedFailureStatesHolder failureStatesHolder;
    private final BucketRangeFinder bucketRangeFinder;
    private final Set<Class<? extends Exception>> recognizedExceptions;

    CircuitBreakerImpl(final BucketedStateManagersHolder stateManagersHolder,
                       final TimeoutScheduler timeoutScheduler,
                       final BucketedFailureStatesHolder failureStatesHolder,
                       final BucketRangeFinder bucketRangeFinder,
                       final Set<Class<? extends Exception>> recognizedExceptions) {
        this.stateManagersHolder = stateManagersHolder;
        this.timeoutScheduler = timeoutScheduler;
        this.failureStatesHolder = failureStatesHolder;
        this.bucketRangeFinder = bucketRangeFinder;
        this.recognizedExceptions = recognizedExceptions;
    }

    @Override
    public <T> CompletableFuture<T> execute(final Supplier<CompletableFuture<T>> futureSupplier,
                                            final Function<Throwable, T> fallback,
                                            final int timeoutInMillis) {

        final BucketRange timeoutBucketRange = bucketRangeFinder.find(timeoutInMillis);

        final Supplier<CompletableFuture<T>> timeoutFutureSupplier = timeoutScheduler.addTimeout(futureSupplier,
                                                                                                 timeoutInMillis,
                                                                                                 timeoutBucketRange);
        final StateManager stateManager = stateManagersHolder.getStateManager(timeoutBucketRange);
        final LastFailureCause lastFailureCause = failureStatesHolder.getFailureState(timeoutBucketRange);

        if (stateManager.isOpen()) {
            final Throwable lastFailure = lastFailureCause.get();
            return new CompletedFuture<>(() -> fallback.apply(lastFailure));
        }

        return timeoutFutureSupplier.get()
                                    .whenComplete(new ResultConsumer<>(stateManager, lastFailureCause, recognizedExceptions));
    }
}
