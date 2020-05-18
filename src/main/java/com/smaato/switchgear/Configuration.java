package com.smaato.switchgear;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import com.smaato.switchgear.circuitbreaker.state.Strategy;
import com.smaato.switchgear.concurrent.ThreadFactoryBuilder;
import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

@SuppressWarnings("ClassWithTooManyFields")
public class Configuration {

    private static final boolean ENABLED_BY_DEFAULT = true;
    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 1000;
    private static final int DEFAULT_FAILURES_ALLOWED = 100;
    private static final int DEFAULT_FAILURES_PERCENTAGE = 80;
    private static final int DEFAULT_THROTTLING_PERCENTAGE = 80;
    private static final int DEFAULT_WINDOW_SIZE = 100;
    private static final int DEFAULT_CIRCUIT_OPEN_TIME_IN_MILLIS = 1000;
    private static final String DEFAULT_ISOLATION_THREAD_NAME = "switchgear-isolation";
    private static final String DEFAULT_STATE_MANAGER_THREAD_NAME = "switchgear-state-manager";
    private static final ThreadFactory ISOLATION_THREAD_FACTORY = ThreadFactoryBuilder.builder()
                                                                                      .withName(DEFAULT_ISOLATION_THREAD_NAME)
                                                                                      .isDaemon()
                                                                                      .build();
    private static final ThreadFactory STATE_MANAGER_THREAD_FACTORY = ThreadFactoryBuilder.builder()
                                                                                          .withName(DEFAULT_STATE_MANAGER_THREAD_NAME)
                                                                                          .isDaemon()
                                                                                          .build();

    private final Executor executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final boolean circuitBreakerEnabled;
    private final int timeoutInMillis;
    private final int maxConsecutiveFailuresAllowed;
    private final int acceptableFailuresPercentage;
    private final int minimumWindowSize;
    private final int throttlingPercentage;
    private final int circuitOpenTimeInMillis;
    private final Integer bucketLengthInMillis;
    private final Set<Class<? extends Exception>> recognizedExceptions;
    private final Strategy stateManagerStrategy;

    private Configuration(final Executor executor,
                          final ScheduledExecutorService scheduledExecutor,
                          final boolean circuitBreakerEnabled,
                          final int timeoutInMillis,
                          final int maxConsecutiveFailuresAllowed,
                          final int acceptableFailuresPercentage,
                          final int minimumWindowSize,
                          final int throttlingPercentage,
                          final int circuitOpenTimeInMillis,
                          final Integer bucketLengthInMillis,
                          final Set<Class<? extends Exception>> recognizedExceptions,
                          final Strategy stateManagerStrategy) {
        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.circuitBreakerEnabled = circuitBreakerEnabled;
        this.timeoutInMillis = timeoutInMillis;
        this.maxConsecutiveFailuresAllowed = maxConsecutiveFailuresAllowed;
        this.acceptableFailuresPercentage = acceptableFailuresPercentage;
        this.minimumWindowSize = minimumWindowSize;
        this.throttlingPercentage = throttlingPercentage;
        this.circuitOpenTimeInMillis = circuitOpenTimeInMillis;
        this.bucketLengthInMillis = bucketLengthInMillis;
        this.recognizedExceptions = recognizedExceptions;
        this.stateManagerStrategy = stateManagerStrategy;
    }

    public Executor getExecutor() {
        return executor;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public Integer getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public int getMaxConsecutiveFailuresAllowed() {
        return maxConsecutiveFailuresAllowed;
    }

    public int getAcceptableFailuresPercentage() {
        return acceptableFailuresPercentage;
    }

    public int getMinimumWindowSize() {
        return minimumWindowSize;
    }

    public int getCircuitOpenTimeInMillis() {
        return circuitOpenTimeInMillis;
    }

    public Integer getBucketLengthInMillis() {
        return bucketLengthInMillis;
    }

    public Set<Class<? extends Exception>> getRecognizedExceptions() {
        return recognizedExceptions;
    }

    public Strategy getStateManagerStrategy() {
        return stateManagerStrategy;
    }

    public int getThrottlingPercentage() {
        return throttlingPercentage;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("ClassWithTooManyMethods")
    public static class Builder {
        private Executor executor;
        private ScheduledExecutorService scheduledExecutor;
        private int defaultTimeoutInMillis = DEFAULT_TIMEOUT_IN_MILLIS;
        private boolean circuitBreakerEnabled = ENABLED_BY_DEFAULT;
        private int maxConsecutiveFailuresAllowed = DEFAULT_FAILURES_ALLOWED;
        private int failuresPercentage = DEFAULT_FAILURES_PERCENTAGE;
        private int minimumWindowSize = DEFAULT_WINDOW_SIZE;
        private int circuitOpenTimeInMillis = DEFAULT_CIRCUIT_OPEN_TIME_IN_MILLIS;
        private int throttlingPercentage = DEFAULT_THROTTLING_PERCENTAGE;
        private Integer bucketLengthInMillis;
        private Set<Class<? extends Exception>> recognizedExceptions = Collections.emptySet();
        private Strategy stateManagerStrategy = Strategy.CONSECUTIVE_FAILURES;

        /**
         * @param executor for thread level isolation of {@link Action} executions.
         * <p>Every {@link Action#getCall()} is executed in a separate thread.</p>
         * <p>Uses {@link Executors#newCachedThreadPool()} by default.</p>
         * @throws NullPointerException if executor is null.
         */
        public Builder withExecutor(final Executor executor) {
            requireNonNull(executor);
            this.executor = executor;
            return this;
        }

        /**
         * @param scheduledExecutor for timeout handler events.
         * <p>
         * Circuit breaker internally is using {@link ScheduledExecutorService} to schedule timeout events
         * and manage its internal state.
         * </p>
         * <p>Uses {@link Executors#newSingleThreadScheduledExecutor()} by default.</p>
         * @throws NullPointerException if scheduledExecutor is null.
         */
        public Builder withScheduledExecutor(final ScheduledExecutorService scheduledExecutor) {
            requireNonNull(scheduledExecutor);
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        /**
         * @param defaultTimeoutInMillis timeout for {@link Action#getCall()} in milliseconds.
         * <p>
         * After this timeout reached, the {@link Outcome} will be a failure with {@link TimeoutException}.
         * Circuit breaker is also using this timeout to detect failures.
         * </p>
         * <p>Defaults to 1000.</p>
         * @throws IllegalArgumentException if defaultTimeoutInMillis less or equal to zero.
         */
        public Builder withDefaultTimeoutInMillis(final int defaultTimeoutInMillis) {
            if (defaultTimeoutInMillis <= 0) {
                throw new IllegalArgumentException();
            }
            this.defaultTimeoutInMillis = defaultTimeoutInMillis;
            return this;
        }

        /**
         * @param circuitBreakerEnabled feature toggle for internal circuit breaker.
         * <p>Is enabled by default.</p>
         */
        public Builder withCircuitBreakerEnabled(final boolean circuitBreakerEnabled) {
            this.circuitBreakerEnabled = circuitBreakerEnabled;
            return this;
        }

        /**
         * @param maxConsecutiveFailuresAllowed configuration parameter for circuit breaker based on {@link Strategy#CONSECUTIVE_FAILURES}.
         * <p>Sets the number of consecutive failures that will open the circuit.</p>
         * <p>Default value is 100.</p>
         * @throws IllegalArgumentException if maxConsecutiveFailuresAllowed less or equal to zero.
         */
        public Builder withMaxConsecutiveFailuresAllowed(final int maxConsecutiveFailuresAllowed) {
            if (maxConsecutiveFailuresAllowed <= 0) {
                throw new IllegalArgumentException();
            }
            this.maxConsecutiveFailuresAllowed = maxConsecutiveFailuresAllowed;
            return this;
        }

        /**
         * @param failuresPercentage configuration parameter for circuit breaker based on {@link Strategy#FREQUENT_FAILURES}.
         * <p>
         * Sets the threshold for the percentage of failures, if the amount of failures exceeds this threshold
         * circuit breaker will block following calls.
         * </p>
         * <p>Default value is 80.</p>
         * @throws IllegalArgumentException if failuresPercentage is not in the range of [1, 100]
         */
        public Builder withFailuresPercentage(final int failuresPercentage) {
            if ((failuresPercentage < 1) || (failuresPercentage > 100)) {
                throw new IllegalArgumentException();
            }
            this.failuresPercentage = failuresPercentage;
            return this;
        }

        /**
         * @param minimumWindowSize configuration parameter for circuit breaker based on {@link Strategy#FREQUENT_FAILURES}.
         * <p>Sets the minimum number of calls to be able to calculate the percentage of failures.</p>
         * <p>Default value is 100.</p>
         * @throws IllegalArgumentException if minimumWindowSize less or equal to zero.
         */
        public Builder withMinimumWindowSize(final int minimumWindowSize) {
            if (minimumWindowSize <= 0) {
                throw new IllegalArgumentException();
            }
            this.minimumWindowSize = minimumWindowSize;
            return this;
        }

        /**
         * @param throttlingPercentage configuration parameter for circuit breaker based on {@link Strategy#FREQUENT_FAILURES}.
         * <p>Sets the percentage of call that will be blocked by circuit breaker.</p>
         * <p>Default value is 80.</p>
         * @throws IllegalArgumentException if throttlingPercentage is not in the range of [1, 100]
         */
        public Builder withThrottlingPercentage(final int throttlingPercentage) {
            if ((throttlingPercentage < 1) || (throttlingPercentage > 100)) {
                throw new IllegalArgumentException();
            }
            this.throttlingPercentage = throttlingPercentage;
            return this;
        }

        /**
         * @param circuitOpenTimeInMillis configuration parameter for circuit breaker.
         * <p>Defines after how many milliseconds to half-open the circuit in case of {@link Strategy#CONSECUTIVE_FAILURES}</p>
         * <p>In case of {@link Strategy#FREQUENT_FAILURES} it defines the period over which failure percentage is being calculated.</p>
         * <p>Default value is 1000.</p>
         * @throws IllegalArgumentException if circuitOpenTimeInMillis less or equal to zero.
         */
        public Builder withCircuitOpenTimeInMillis(final int circuitOpenTimeInMillis) {
            if (circuitOpenTimeInMillis <= 0) {
                throw new IllegalArgumentException();
            }
            this.circuitOpenTimeInMillis = circuitOpenTimeInMillis;
            return this;
        }

        /**
         * @param bucketLengthInMillis configuration parameter for circuit breaker.
         * <p>Setting bucketLengthInMillis enables a multi bucket circuit breaker of the given size.</p>
         * <p>Single bucket circuit breaker by default.</p>
         * @throws IllegalArgumentException if bucketLengthInMillis is less or equal to zero.
         */
        public Builder withBucketLengthInMillis(final int bucketLengthInMillis) {
            if (bucketLengthInMillis <= 0) {
                throw new IllegalArgumentException();
            }
            this.bucketLengthInMillis = bucketLengthInMillis;
            return this;
        }

        /**
         * @param recognizedExceptions a set of exceptions that are managed by circuit breaker
         * @throws NullPointerException if recognizedExceptions is null
         */
        public Builder withRecognizedExceptions(final Set<Class<? extends Exception>> recognizedExceptions) {
            requireNonNull(recognizedExceptions);
            this.recognizedExceptions = recognizedExceptions;
            return this;
        }

        /**
         * @param stateManagerStrategy defines how circuit breaker open state is triggered.
         * @throws NullPointerException if stateManagerStrategy is null
         */
        public Builder withStateManagerStrategy(final Strategy stateManagerStrategy) {
            requireNonNull(stateManagerStrategy);
            this.stateManagerStrategy = stateManagerStrategy;
            return this;
        }

        public Configuration build() {
            return new Configuration(getOrDefault(executor, Executors.newCachedThreadPool(ISOLATION_THREAD_FACTORY)),
                                     getOrDefault(scheduledExecutor,
                                                  Executors.newSingleThreadScheduledExecutor(STATE_MANAGER_THREAD_FACTORY)),
                                     circuitBreakerEnabled,
                                     defaultTimeoutInMillis,
                                     maxConsecutiveFailuresAllowed,
                                     failuresPercentage,
                                     minimumWindowSize,
                                     throttlingPercentage,
                                     circuitOpenTimeInMillis,
                                     bucketLengthInMillis,
                                     recognizedExceptions,
                                     stateManagerStrategy);
        }

        private static <E> E getOrDefault(final E value,
                                          final E defaultValue) {
            return (value == null) ? defaultValue : value;
        }
    }
}
