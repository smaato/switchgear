package com.smaato.switchgear.circuitbreaker.state;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsecutiveFailuresStateManager implements StateManager {
    private static final int NO_DELAY = 0;

    private final int maxConsecutiveFailuresAllowed;

    private final AtomicInteger consecutiveFailureCount = new AtomicInteger(0);

    private ConsecutiveFailuresStateManager(final int maxConsecutiveFailuresAllowed) {
        this.maxConsecutiveFailuresAllowed = maxConsecutiveFailuresAllowed;
    }

    public static StateManager newInstance(final int maxConsecutiveFailuresAllowed,
                                           final int circuitOpenTimeInMillis,
                                           final ScheduledExecutorService scheduledExecutor) {
        final ConsecutiveFailuresStateManager stateManager = new ConsecutiveFailuresStateManager(maxConsecutiveFailuresAllowed);
        scheduledExecutor.scheduleAtFixedRate(stateManager::halfOpenCheck, NO_DELAY, circuitOpenTimeInMillis, MILLISECONDS);
        return stateManager;
    }

    @Override
    public boolean isOpen() {
        return consecutiveFailureCount.get() >= maxConsecutiveFailuresAllowed;
    }

    @Override
    public void handleSuccess() {
        consecutiveFailureCount.set(0);
    }

    @Override
    public void handleFailure() {
        consecutiveFailureCount.incrementAndGet();
    }

    private void halfOpenCheck() {
        if (consecutiveFailureCount.get() >= maxConsecutiveFailuresAllowed) {
            consecutiveFailureCount.set(maxConsecutiveFailuresAllowed - 1);
        }
    }
}
