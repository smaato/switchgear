package com.smaato.switchgear;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

@SuppressWarnings("unchecked")
public class SwitchgearTest {

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 100;
    private static final int TIMEOUT_IN_MILLIS = 50;

    private final ExecutorService executorServiceMock = mock(ExecutorService.class);
    private final ResultCollector resultCollectorMock = mock(ResultCollector.class);

    private final Switchgear switchgear = new Switchgear(executorServiceMock,
                                                         resultCollectorMock,
                                                         DEFAULT_TIMEOUT_IN_MILLIS);

    private final Action<Object> actionMock = mock(Action.class);
    private final Collection<Action<Object>> actionMocks = Collections.singleton(actionMock);
    private final CompletableFuture<Object> futureMock = mock(CompletableFuture.class);
    private final Outcome<Object> expectedOutcome = mock(Outcome.class);

    @Test
    public void whenRequestedWithTimeoutThenUseIt() {
        when(executorServiceMock.execute(actionMock, TIMEOUT_IN_MILLIS)).thenReturn(futureMock);
        when(resultCollectorMock.getOutcome(actionMock, futureMock, TIMEOUT_IN_MILLIS)).thenReturn(expectedOutcome);
        when(actionMock.getTimeoutInMillis()).thenReturn(Optional.of(TIMEOUT_IN_MILLIS));

        final Outcome<Object> actualOutcome = switchgear.execute(actionMock);

        assertThat(actualOutcome, is(expectedOutcome));
    }

    @Test
    public void whenRequestedWithoutTimeoutThenUseDefault() {
        when(executorServiceMock.execute(actionMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(futureMock);
        when(resultCollectorMock.getOutcome(actionMock, futureMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(expectedOutcome);

        final Outcome<Object> actualOutcome = switchgear.execute(actionMock);

        assertThat(actualOutcome, is(expectedOutcome));
    }

    @Test
    public void whenParallelExecutionRequestedWithTimeoutThenUseIt() {
        when(executorServiceMock.execute(actionMock, TIMEOUT_IN_MILLIS)).thenReturn(futureMock);
        when(resultCollectorMock.getOutcome(actionMock, futureMock, TIMEOUT_IN_MILLIS)).thenReturn(expectedOutcome);
        when(actionMock.getTimeoutInMillis()).thenReturn(Optional.of(TIMEOUT_IN_MILLIS));

        final Collection<Outcome<Object>> actualOutcomes = switchgear.executeInParallel(actionMocks);

        assertThat(actualOutcomes, containsInAnyOrder(expectedOutcome));
    }

    @Test
    public void whenParallelExecutionRequestedWithoutTimeoutThenUseDefault() {
        when(executorServiceMock.execute(actionMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(futureMock);
        when(resultCollectorMock.getOutcome(actionMock, futureMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(expectedOutcome);

        final Collection<Outcome<Object>> actualOutcomes = switchgear.executeInParallel(actionMocks);

        assertThat(actualOutcomes, containsInAnyOrder(expectedOutcome));
    }

    @Test
    public void whenMultipleActionsExecutedThenMultipleOutcomes() {

        final Action<Object> firstActionMock = mock(Action.class);
        final CompletableFuture<Object> firstFutureMock = mock(CompletableFuture.class);
        final Outcome<Object> firstExpectedOutcome = mock(Outcome.class);

        when(executorServiceMock.execute(firstActionMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(firstFutureMock);
        when(resultCollectorMock.getOutcome(firstActionMock, firstFutureMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(firstExpectedOutcome);

        final Action<Object> secondActionMock = mock(Action.class);
        final CompletableFuture<Object> secondFutureMock = mock(CompletableFuture.class);
        final Outcome<Object> secondExpectedOutcome = mock(Outcome.class);

        when(executorServiceMock.execute(secondActionMock, DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(secondFutureMock);
        when(resultCollectorMock.getOutcome(secondActionMock,
                                            secondFutureMock,
                                            DEFAULT_TIMEOUT_IN_MILLIS)).thenReturn(secondExpectedOutcome);

        final Collection<Outcome<Object>> actualOutcomes = switchgear.executeInParallel(Arrays.asList(firstActionMock, secondActionMock));

        assertThat(actualOutcomes, containsInAnyOrder(firstExpectedOutcome, secondExpectedOutcome));
    }

    @Test(expected = NullPointerException.class)
    public void whenRequestedNewSwitchgearInstanceWithNullConfigurationThenThrowException() {
        Switchgear.newInstance(null);
    }

    @Test(expected = NullPointerException.class)
    public void whenExecutedNullActionThenThrowException() {
        switchgear.execute(null);
    }

    @Test(expected = NullPointerException.class)
    public void whenExecutedParallelNullActionThenThrowException() {
        switchgear.executeInParallel(null);
    }
}