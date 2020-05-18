package com.smaato.switchgear.circuitbreaker.state;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.smaato.switchgear.circuitbreaker.state.bucket.ThrottlingUtil;

public class FrequentFailuresStateManager implements StateManager {

    private static final double HUNDRED_PERCENT = 100.0;

    private enum State {OPEN, CLOSED, THROTTLE, HALF_OPEN}

    private final int acceptableFailuresPercentage;
    private final int minWindowSize;
    private final ThrottlingUtil throttlingUtil;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    private FrequentFailuresStateManager(final int acceptableFailuresPercentage,
                                         final int minWindowSize,
                                         final ThrottlingUtil throttlingUtil) {
        this.acceptableFailuresPercentage = acceptableFailuresPercentage;
        this.minWindowSize = minWindowSize;
        this.throttlingUtil = throttlingUtil;
    }

    public static StateManager newInstance(final int acceptableFailuresPercentage,
                                           final int circuitOpenTimeInMillis,
                                           final int minWindowSize,
                                           final ThrottlingUtil throttlingUtil,
                                           final ScheduledExecutorService scheduledExecutor) {
        final FrequentFailuresStateManager stateManager = new FrequentFailuresStateManager(acceptableFailuresPercentage,
                                                                                           minWindowSize,
                                                                                           throttlingUtil);
        scheduledExecutor.scheduleAtFixedRate(stateManager::updateState,
                                              circuitOpenTimeInMillis,
                                              circuitOpenTimeInMillis,
                                              MILLISECONDS);
        return stateManager;
    }

    @Override
    public boolean isOpen() {
        if (state.get() == State.THROTTLE) {
            return throttlingUtil.throttle();
        }
        return state.get() == State.OPEN;
    }

    @Override
    public void handleSuccess() {
        state.compareAndSet(State.HALF_OPEN, State.CLOSED);
        successCount.incrementAndGet();
    }

    @Override
    public void handleFailure() {
        state.compareAndSet(State.HALF_OPEN, State.OPEN);
        failureCount.incrementAndGet();
    }

    private void updateState() {
        final int currentFailureCount = failureCount.get();
        final int totalCount = currentFailureCount + successCount.get();

        if (totalCount >= minWindowSize) {
            final double failuresPercentage = (currentFailureCount * HUNDRED_PERCENT) / totalCount;

            if (currentFailureCount == totalCount) {
                state.set(State.OPEN);
            } else if (failuresPercentage > acceptableFailuresPercentage) {
                state.set(State.THROTTLE);
            } else {
                state.set(State.CLOSED);
            }

            failureCount.set(0);
            successCount.set(0);
        } else {
            state.compareAndSet(State.OPEN, State.HALF_OPEN);
        }
    }
}
