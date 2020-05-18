package com.smaato.switchgear.circuitbreaker.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.state.bucket.ThrottlingUtil;

public class FrequentFailuresStateManagerTest {
    private static final int FAILURES_PERCENTAGE = 50;
    private static final int WINDOW_SIZE = 100;
    private static final int CIRCUIT_OPEN_TIME_IN_MILLIS = 100;
    private static final int EXTRA_TIME = 50;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final ThrottlingUtil throttlingUtilMock = mock(ThrottlingUtil.class);

    private final StateManager stateManager = FrequentFailuresStateManager.newInstance(FAILURES_PERCENTAGE,
                                                                                       CIRCUIT_OPEN_TIME_IN_MILLIS,
                                                                                       WINDOW_SIZE,
                                                                                       throttlingUtilMock,
                                                                                       scheduledExecutor);
    @Before
    public void setUp() {
        when(throttlingUtilMock.throttle()).thenReturn(true);
    }

    @Test
    public void givenInitialStateThenCircuitIsClosed() {
        assertThat(stateManager.isOpen()).isFalse();
        verify(throttlingUtilMock, never()).throttle();
    }

    @Test
    public void whenTooManyFailuresThenThrottle() throws InterruptedException {
        failMostOfRequests();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isTrue();
        verify(throttlingUtilMock).throttle();
    }

    @Test
    public void whenAllFailedThenOpenCircuit() throws InterruptedException {
        failAllRequests();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isTrue();
        assertThat(stateManager.isOpen()).isTrue();

        verify(throttlingUtilMock, never()).throttle();
    }

    @Test
    public void whenTooManyFailuresButUpdateTimeNotReachedThenCircuitIsStillClosed() {
        failAllRequests();

        assertThat(stateManager.isOpen()).isFalse();
        verify(throttlingUtilMock, never()).throttle();
    }

    @Test
    public void whenEnoughSuccessThenCircuitRemainsClosed() throws InterruptedException {
        final int half = WINDOW_SIZE / 2;
        for (int i = 0; i < half; i++) {
            assertThat(stateManager.isOpen()).isFalse();
            stateManager.handleFailure();
        }
        for (int i = 0; i < half; i++) {
            assertThat(stateManager.isOpen()).isFalse();
            stateManager.handleSuccess();
        }

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isFalse();
        verify(throttlingUtilMock, never()).throttle();
    }

    @Test
    public void whenSuccessAfterThrottlingThenCloseCircuit() throws InterruptedException {
        failMostOfRequests();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isTrue();

        for (int i = 0; i < WINDOW_SIZE; i++) {
            assertThat(stateManager.isOpen()).isTrue();
            stateManager.handleSuccess();
        }

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isFalse();
    }

    @Test
    public void whenSuccessAfterCircuitWasOpenThenCloseCircuit() throws InterruptedException {
        failAllRequests();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isTrue();
        verify(throttlingUtilMock, never()).throttle();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);
        assertThat(stateManager.isOpen()).isFalse();
        stateManager.handleSuccess();
        assertThat(stateManager.isOpen()).isFalse();
    }

    @Test
    public void whenFailureAfterCircuitWasOpenThenKeepOpen() throws InterruptedException {
        failAllRequests();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);

        assertThat(stateManager.isOpen()).isTrue();
        verify(throttlingUtilMock, never()).throttle();

        Thread.sleep(CIRCUIT_OPEN_TIME_IN_MILLIS + EXTRA_TIME);
        assertThat(stateManager.isOpen()).isFalse();
        stateManager.handleFailure();
        assertThat(stateManager.isOpen()).isTrue();
    }

    private void failMostOfRequests() {
        final int moreThenHalf = (WINDOW_SIZE / 2) + 1;
        for (int i = 0; i < moreThenHalf; i++) {
            assertThat(stateManager.isOpen()).isFalse();
            stateManager.handleFailure();
        }
        final int lessThenHalf = (WINDOW_SIZE / 2) - 1;
        for (int i = 0; i < lessThenHalf; i++) {
            assertThat(stateManager.isOpen()).isFalse();
            stateManager.handleSuccess();
        }
    }

    private void failAllRequests() {
        for (int i = 0; i < WINDOW_SIZE; i++) {
            assertThat(stateManager.isOpen()).isFalse();
            stateManager.handleFailure();
        }
    }
}