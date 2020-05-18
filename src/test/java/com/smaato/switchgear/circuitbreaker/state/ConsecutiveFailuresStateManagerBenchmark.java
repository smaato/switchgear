package com.smaato.switchgear.circuitbreaker.state;

import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.All)
@Threads(10)
public class ConsecutiveFailuresStateManagerBenchmark {

    private static final int ONE_SECOND = 1000;
    private static final int MAX_CONSECUTIVE_FAILURES_ALLOWED = 100;

    private final ScheduledExecutorService scheduledExecutorMock = mock(ScheduledExecutorService.class);

    private StateManager stateManager;

    @Setup
    public void setup() {
        stateManager = ConsecutiveFailuresStateManager.newInstance(MAX_CONSECUTIVE_FAILURES_ALLOWED, ONE_SECOND, scheduledExecutorMock);
    }

    @Benchmark
    public void measureSuccessRuns(final ConsecutiveFailuresStateManagerBenchmark benchmark) {
        benchmark.stateManager.handleSuccess();
    }

    @Benchmark
    public void measureFailureRuns(final ConsecutiveFailuresStateManagerBenchmark benchmark) {
        benchmark.stateManager.handleFailure();
    }

    @Benchmark
    public void isOpen(final ConsecutiveFailuresStateManagerBenchmark benchmark) {
        benchmark.stateManager.isOpen();
    }

    @Benchmark
    public void mixedSuccess(final ConsecutiveFailuresStateManagerBenchmark benchmark) {
        benchmark.stateManager.isOpen();
        benchmark.stateManager.handleSuccess();
    }
}
