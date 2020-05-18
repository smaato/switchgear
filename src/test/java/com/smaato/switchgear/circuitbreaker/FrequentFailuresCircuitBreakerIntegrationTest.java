package com.smaato.switchgear.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.Configuration.Builder;
import com.smaato.switchgear.circuitbreaker.state.Strategy;

public class FrequentFailuresCircuitBreakerIntegrationTest {
    private static final String RESULT = "RESULT";
    private static final String FALLBACK_RESULT = "FALLBACK_RESULT";
    private static final int TIMEOUT = 50;
    private static final int CIRCUIT_OPEN_TIME_IN_MILLIS = 100;
    private static final int TIME_LONGER_THAN_HALF_OPEN_TIME = CIRCUIT_OPEN_TIME_IN_MILLIS + 2;
    private static final int FAILURES_PERCENTAGE = 50;
    private static final int WINDOW_SIZE = 100;
    private static final int THROTTLE_HALF = 50;

    private final Supplier<CompletableFuture<String>> sleepySupplier = () -> CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException ignore) {
        }
        return RESULT;
    });

    private final Supplier<CompletableFuture<String>> failedSupplier =
            () -> CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException();
            });
    private final Supplier<CompletableFuture<String>> successfulSupplier = () -> CompletableFuture.completedFuture(RESULT);

    private final Function<Throwable, String> fallbackFunction = (throwable) -> FALLBACK_RESULT;

    private final Builder baseConfig = Configuration.builder()
                                                    .withCircuitOpenTimeInMillis(CIRCUIT_OPEN_TIME_IN_MILLIS)
                                                    .withStateManagerStrategy(Strategy.FREQUENT_FAILURES)
                                                    .withFailuresPercentage(FAILURES_PERCENTAGE)
                                                    .withMinimumWindowSize(WINDOW_SIZE);

    private final CircuitBreaker throttlingCircuitBreaker = CircuitBreakerFactory.newInstance(
            baseConfig.withThrottlingPercentage(THROTTLE_HALF)
                      .build()
    );

    @Test
    public void whenFirstRequestThenCircuitIsClosed() throws Exception {
        final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(RESULT);
    }

    @Test
    public void whenTooManyExceptionThenCircuitIsOpenAndFallbackReturned() throws Exception {
        failMostOfRequests(throttlingCircuitBreaker);

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        // Triple requests because of throttling
        int fallbackCount = 0;
        int resultCount = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

            if (future.get().equals(FALLBACK_RESULT)) {
                fallbackCount++;
            }
            if (future.get().equals(RESULT)) {
                resultCount++;
            }
        }

        // Ensure throttling happened
        assertThat(fallbackCount).isGreaterThan(0);
        assertThat(resultCount).isGreaterThan(0);
    }

    @Test
    public void whenFailuresAreNotAboveThresholdThenCircuitIsClosed() throws Exception {
        // Failing calls (exactly 50%)
        final int half = WINDOW_SIZE / 2;
        for (int i = 0; i < half; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        // Successful calls (exactly 50%)
        for (int i = 0; i < half; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

            assertThat(future.get()).isEqualTo(RESULT);
        }

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(RESULT);
    }

    @Test
    public void whenTooManyTimeoutsThenCircuitIsOpenAndFallbackReturned() throws Exception {
        final Consumer<ExecutionException> timeoutExceptionRequirement = e -> assertThat(e.getCause()).isInstanceOf(TimeoutException.class);

        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(sleepySupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement);
        }

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(FALLBACK_RESULT);
    }

    @Test
    public void whenSuccessfulAfterThrottlingThenCloseCircuit() throws Exception {
        failMostOfRequests(throttlingCircuitBreaker);

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        // Triple requests because of throttling
        int fallbackCount = 0;
        int resultCount = 0;
        for (int i = 0; i < (WINDOW_SIZE * 3); i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

            if (future.get().equals(FALLBACK_RESULT)) {
                fallbackCount++;
            }
            if (future.get().equals(RESULT)) {
                resultCount++;
            }
        }

        // Ensure throttling happened
        assertThat(fallbackCount).isGreaterThan(0);
        assertThat(resultCount).isGreaterThan(0);

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        // Ensure no throttling
        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);
            assertThat(future.get()).isEqualTo(RESULT);
        }
    }

    @Test
    public void whenSuccessfulAfterConsecutiveFailuresThenCloseCircuit() throws Exception {
        // Fail all of the requests
        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        // No throttling circuit remains open
        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);
            assertThat(future.get()).isEqualTo(FALLBACK_RESULT);
        }

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        // Close circuit with the successful half-open request
        final CompletableFuture<String> halfOpenRequest = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);
        assertThat(halfOpenRequest.get()).isEqualTo(RESULT);

        // Ensure no throttling
        for (int i = 0; i < WINDOW_SIZE; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);
            assertThat(future.get()).isEqualTo(RESULT);
        }
    }

    private void failMostOfRequests(final CircuitBreaker throttlingCircuitBreaker) throws Exception {
        // Failing calls (more then 50%)
        final int moreThenHalf = (WINDOW_SIZE / 2) + 1;
        for (int i = 0; i < moreThenHalf; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        // Successful calls (less then 50%)
        final int lessThenHalf = (WINDOW_SIZE / 2) - 1;
        for (int i = 0; i < lessThenHalf; i++) {
            final CompletableFuture<String> future = throttlingCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

            assertThat(future.get()).isEqualTo(RESULT);
        }
    }
}
