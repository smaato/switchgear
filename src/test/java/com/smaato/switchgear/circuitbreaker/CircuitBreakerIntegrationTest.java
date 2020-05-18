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

public class CircuitBreakerIntegrationTest {

    private static final String RESULT = "RESULT";
    private static final String FALLBACK_RESULT = "FALLBACK_RESULT";

    private static final int MAX_CONSECUTIVE_FAILURES_ALLOWED = 2;
    private static final int TIMEOUT = 50;
    private static final int CIRCUIT_OPEN_TIME_IN_MILLIS = 100;

    private static final int TIME_LONGER_THAN_HALF_OPEN_TIME = CIRCUIT_OPEN_TIME_IN_MILLIS + 2;

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

    private final CircuitBreaker singleBucketCircuitBreaker = CircuitBreakerFactory.newInstance(
            Configuration.builder()
                         .withCircuitOpenTimeInMillis(CIRCUIT_OPEN_TIME_IN_MILLIS)
                         .withMaxConsecutiveFailuresAllowed(MAX_CONSECUTIVE_FAILURES_ALLOWED)
                         .build()
    );

    private final CircuitBreaker multiBucketCircuitBreaker = CircuitBreakerFactory.newInstance(
            Configuration.builder()
                         .withCircuitOpenTimeInMillis(CIRCUIT_OPEN_TIME_IN_MILLIS)
                         .withMaxConsecutiveFailuresAllowed(MAX_CONSECUTIVE_FAILURES_ALLOWED)
                         .withBucketLengthInMillis(50)
                         .build());

    @Test
    public void whenFirstRequestThenCircuitIsClosed() throws Exception {
        final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(RESULT);
    }

    @Test
    public void whenActionThrowsExceptionThenCircuitIsOpenAndFallbackReturned() throws Exception {

        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(FALLBACK_RESULT);
    }

    @Test
    public void whenActionTimeoutThenCircuitIsOpenAndFallbackReturned() throws Exception {
        final Consumer<ExecutionException> timeoutExceptionRequirement = e -> assertThat(e.getCause()).isInstanceOf(TimeoutException.class);

        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(sleepySupplier, fallbackFunction, TIMEOUT);

            assertThatThrownBy(future::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement);
        }

        final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(FALLBACK_RESULT);
    }

    @Test
    public void whenCircuitIsOpenThenHalfOpenAfterDelay() throws Exception {
        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);
            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT);

        assertThat(future.get()).isEqualTo(FALLBACK_RESULT);

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        final Object actualResult = singleBucketCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT).get();

        assertThat(actualResult).isEqualTo(RESULT);
    }

    @Test
    public void whenFailOnHalfOpenThenHalfOpenAgainAfterTimeout() throws Exception {

        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);
            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);
        }

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        final CompletableFuture<String> future = singleBucketCircuitBreaker.execute(failedSupplier, fallbackFunction, TIMEOUT);
        assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class);

        Thread.sleep(TIME_LONGER_THAN_HALF_OPEN_TIME);

        final Object actualResult = singleBucketCircuitBreaker.execute(successfulSupplier, fallbackFunction, TIMEOUT).get();
        assertThat(actualResult).isEqualTo(RESULT);
    }

    @Test
    public void whenCircuitOpenForOneBucketThenOtherBucketsAreNotAffected() throws Exception {
        final Consumer<ExecutionException> timeoutExceptionRequirement = e -> assertThat(e.getCause()).isInstanceOf(TimeoutException.class);

        final int firstBucketTimeOut = 40;
        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> firstBucketFuture = multiBucketCircuitBreaker.execute(sleepySupplier,
                                                                                                  fallbackFunction,
                                                                                                  firstBucketTimeOut);

            assertThatThrownBy(firstBucketFuture::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement);
        }

        final CompletableFuture<String> firstBucketFuture = multiBucketCircuitBreaker.execute(sleepySupplier,
                                                                                              fallbackFunction,
                                                                                              firstBucketTimeOut);
        assertThat(firstBucketFuture.get()).isEqualTo(FALLBACK_RESULT);

        final int secondBucketTimeout = 150;
        final CompletableFuture<String> future = multiBucketCircuitBreaker.execute(successfulSupplier,
                                                                                   fallbackFunction,
                                                                                   secondBucketTimeout);

        assertThat(future.get()).isEqualTo(RESULT);
    }

    @Test
    public void whenOpenBucketAndAnotherTimeoutInSameBucketThenFallback() throws Exception {
        final Consumer<ExecutionException> timeoutExceptionRequirement = e -> assertThat(e.getCause()).isInstanceOf(TimeoutException.class);

        final int firstBucketFirstTimeOut = 35;
        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = multiBucketCircuitBreaker.execute(sleepySupplier,
                                                                                       fallbackFunction,
                                                                                       firstBucketFirstTimeOut);

            assertThatThrownBy(future::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement);
        }

        final int firstBucketSecondTimeout = 40;
        final CompletableFuture<String> future = multiBucketCircuitBreaker.execute(successfulSupplier,
                                                                                   fallbackFunction,
                                                                                   firstBucketSecondTimeout);

        assertThat(future.get()).isEqualTo(FALLBACK_RESULT);
    }

    @Test
    public void whenCircuitBreakerTimeoutForBucketedCircuitBreakerThenIncludeBucketRangeInErrorCause() throws Exception {
        final Consumer<ExecutionException> timeoutExceptionRequirement = e -> assertThat(e.getCause()).isInstanceOf(TimeoutException.class);

        final int firstBucketFirstTimeOut = 35;
        for (int i = 0; i < MAX_CONSECUTIVE_FAILURES_ALLOWED; i++) {
            final CompletableFuture<String> future = multiBucketCircuitBreaker.execute(sleepySupplier,
                                                                                       fallbackFunction,
                                                                                       firstBucketFirstTimeOut);

            assertThatThrownBy(future::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement)
                                           .hasMessageContaining("Circuit breaker timeout for timeout bucket 0-49 ms");
        }

        final int firstBucketSecondTimeout = 55;
        final CompletableFuture<String> future = multiBucketCircuitBreaker.execute(sleepySupplier,
                                                                                   fallbackFunction,
                                                                                   firstBucketSecondTimeout);

        assertThatThrownBy(future::get).isInstanceOfSatisfying(ExecutionException.class, timeoutExceptionRequirement)
                                       .hasMessageContaining("Circuit breaker timeout for timeout bucket 50-99 ms");
    }
}