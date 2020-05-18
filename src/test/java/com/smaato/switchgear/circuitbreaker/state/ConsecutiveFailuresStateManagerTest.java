package com.smaato.switchgear.circuitbreaker.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;

public class ConsecutiveFailuresStateManagerTest {

    private static final int MAX_CONSECUTIVE_FAILURES_ALLOWED = 5;
    private static final int CIRCUIT_OPEN_TIME_IN_MILLIS = 100;
    private static final int EXTRA_TIME = 5;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final StateManager stateManager = ConsecutiveFailuresStateManager.newInstance(MAX_CONSECUTIVE_FAILURES_ALLOWED,
                                                                                          CIRCUIT_OPEN_TIME_IN_MILLIS,
                                                                                          scheduledExecutor);

    @Test
    public void whenMaxConsecutiveFailuresNumberIsExceedThenOpen() {
        assertThat(stateManager.isOpen()).isFalse();

        exceedMaxFailures();

        assertThat(stateManager.isOpen()).isTrue();
    }

    @Test
    public void whenSuccessThenClose() {
        exceedMaxFailures();

        assertThat(stateManager.isOpen()).isTrue();

        stateManager.handleSuccess();

        assertThat(stateManager.isOpen()).isFalse();
    }

    @Test
    public void whenAlreadyOpenAndFailsThenScheduleNewHalfOpenTime() throws Exception {
        exceedMaxFailures();

        assertThat(stateManager.isOpen()).isTrue();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);
        assertThat(stateManager.isOpen()).isFalse();

        stateManager.handleFailure();
        assertThat(stateManager.isOpen()).isTrue();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);
        assertThat(stateManager.isOpen()).isFalse();
    }

    private void exceedMaxFailures() {
        for (int i = 0; i < (MAX_CONSECUTIVE_FAILURES_ALLOWED - 1); i++) {
            stateManager.handleFailure();
            assertThat(stateManager.isOpen()).isFalse();
        }
        stateManager.handleFailure();
    }
}