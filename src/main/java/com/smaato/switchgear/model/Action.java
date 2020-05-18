package com.smaato.switchgear.model;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.smaato.switchgear.circuitbreaker.CircuitBreakerOpenException;

public class Action<T> {
    private final String groupName;
    private final Callable<T> call;
    private final Function<Throwable, T> circuitBreakerFallback;
    private final Function<Throwable, T> failureFallback;
    private final Integer timeoutInMillis;

    private Action(final String groupName,
                   final Callable<T> call,
                   final Function<Throwable, T> circuitBreakerFallback,
                   final Function<Throwable, T> failureFallback,
                   final Integer timeoutInMillis) {
        this.groupName = groupName;
        this.call = call;
        this.circuitBreakerFallback = circuitBreakerFallback;
        this.failureFallback = failureFallback;
        this.timeoutInMillis = timeoutInMillis;
    }

    /**
     * Create action from provided {@link Callable} with default values for group name and circuit breaker fallback.
     *
     * @param call that will be executed in a separate thread and behind the circuit breaker, may throw checked exceptions.
     * @param <T> action result type.
     * @return action with default group name and default circuit breaker fallback.
     * @throws NullPointerException if call is null.
     */
    public static <T> Action<T> from(final Callable<T> call) {
        Objects.requireNonNull(call);
        return new Builder<>(call).build();
    }

    /**
     * Create action builder with provided {@link Callable}.
     *
     * @param call that will be executed in a separate thread and behind the circuit breaker, may throw checked exceptions.
     * @param <T> action result type.
     * @return action builder to specify non-default values for group name and circuit breaker fallback.
     * @throws NullPointerException if call is null.
     */
    public static <T> Builder<T> builder(final Callable<T> call) {
        Objects.requireNonNull(call);
        return new Builder<>(call);
    }

    public String getGroupName() {
        return groupName;
    }

    public Callable<T> getCall() {
        return call;
    }

    public Function<Throwable, T> getCircuitBreakerFallback() {
        return circuitBreakerFallback;
    }

    public Optional<Function<Throwable, T>> getFailureFallback() {
        return Optional.ofNullable(failureFallback);
    }

    public Optional<Integer> getTimeoutInMillis() {
        return Optional.ofNullable(timeoutInMillis);
    }

    public static class Builder<T> {
        private static final String DEFAULT_GROUP_NAME = "";

        private static <T> T defaultCircuitBreakerFallback(final Throwable throwable) {
            throw new CircuitBreakerOpenException(throwable);
        }

        private final Callable<T> call;
        private String groupName = DEFAULT_GROUP_NAME;
        private Function<Throwable, T> circuitBreakerFallback = Builder::defaultCircuitBreakerFallback;
        private Function<Throwable, T> failureFallback;
        private Integer timeoutInMillis;

        private Builder(final Callable<T> call) {
            this.call = call;
        }

        /**
         * Actions with distinct group names will have distinct circuit breakers.
         *
         * @param groupName shared between multiple actions. For example: remote service endpoint.
         * <p>
         * By default it is empty string.
         * </p>
         * @throws NullPointerException if groupName is null.
         */
        public Builder<T> withGroupName(final String groupName) {
            Objects.requireNonNull(groupName);
            this.groupName = groupName;
            return this;
        }

        /**
         * Provide fallback function for circuit breaker. This fallback will be evoked when circuit is open.
         *
         * @param circuitBreakerFallback fallback function. As an argument it gets failure that caused circuit to be open
         * and provides fallback result for action execution.
         * <p>
         * By default fallback is throwing {@link CircuitBreakerOpenException} resulting in an {@link Outcome}
         * with this exception as the failure.
         * </p>
         * @throws NullPointerException if circuitBreakerFallback is null.
         */
        public Builder<T> withCircuitBreakerFallback(final Function<Throwable, T> circuitBreakerFallback) {
            Objects.requireNonNull(circuitBreakerFallback);
            this.circuitBreakerFallback = circuitBreakerFallback;
            return this;
        }

        /**
         * Provide fallback function for failures. This fallback will be evoked when the action execution resulted in a failure.
         *
         * @param failureFallback fallback function. As an argument it gets failure that happened during the action execution
         * and provides fallback result. In the case when the circuit is open another special circuit breaker fallback will be invoked.
         * When circuit breaker fallback throws an exception then this fallback will be used.
         * <p>
         * By default fallback is null.
         * </p>
         */
        public Builder<T> withFailureFallback(final Function<Throwable, T> failureFallback) {
            this.failureFallback = failureFallback;
            return this;
        }

        /**
         * Timeout for an {@link Action}. After this timeout is reached, the {@link Outcome} will be a failure
         * caused by {@link TimeoutException}.
         * Circuit breaker is also using this timeout exception to detect failures.
         *
         * @param timeoutInMillis in milliseconds.
         * <p>
         * If not set, the default timeout form {@link com.smaato.switchgear.Configuration} is used.
         * </p>
         */
        public Builder<T> withTimeoutInMillis(final Integer timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            return this;
        }

        /**
         * @return new action with provided call, group name and fallback functions.
         */
        public Action<T> build() {
            return new Action<>(groupName, call, circuitBreakerFallback, failureFallback, timeoutInMillis);
        }
    }
}
