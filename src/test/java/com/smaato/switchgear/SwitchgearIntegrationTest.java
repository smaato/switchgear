package com.smaato.switchgear;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.junit.Test;

import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

public class SwitchgearIntegrationTest {

    private static final String SUCCESS_OUTCOME = "Success";
    private static final String FALLBACK_OUTCOME = "Fallback";

    @Test
    public void whenExecuteCalledWithActionWhichCompleteTimelyThenReturnSuccessOutcome() {

        final Switchgear switchgear = SwitchgearFactory.INSTANCE.createFrom(Configuration.builder().build());
        final Action<String> action = Action.builder(() -> SUCCESS_OUTCOME).withCircuitBreakerFallback((t) -> FALLBACK_OUTCOME).build();
        final Outcome<String> outcome = switchgear.execute(action);

        assertTrue(outcome.getValue().isPresent());
        assertThat(outcome.getValue().get(), is(SUCCESS_OUTCOME));
    }

    @Test
    public void whenExecuteThrowsRejectedExecutionExceptionThenReturnUseFallback() {

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Switchgear switchgear = SwitchgearFactory.INSTANCE.createFrom(Configuration.builder()
                                                                                         .withMaxConsecutiveFailuresAllowed(1)
                                                                                         .withExecutor(executorService)
                                                                                         .build());

        executorService.shutdown();

        final Action<String> action = Action.builder(() -> SUCCESS_OUTCOME).withCircuitBreakerFallback((t) -> FALLBACK_OUTCOME).build();
        final Outcome<String> rejectedExecutionFailureOutcome = switchgear.execute(action);

        assertFalse(rejectedExecutionFailureOutcome.getValue().isPresent());
        assertTrue(rejectedExecutionFailureOutcome.getFailure().isPresent());
        assertTrue(rejectedExecutionFailureOutcome.getFailure().get() instanceof RejectedExecutionException);

        final Outcome<String> fallbackOutcome = switchgear.execute(action);

        assertTrue(fallbackOutcome.getValue().isPresent());
        assertThat(fallbackOutcome.getValue().get(), is(FALLBACK_OUTCOME));
    }
}