package com.smaato.switchgear.circuitbreaker.state.bucket;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.circuitbreaker.state.ConsecutiveFailuresStateManager;
import com.smaato.switchgear.circuitbreaker.state.FrequentFailuresStateManager;
import com.smaato.switchgear.circuitbreaker.state.StateManager;
import com.smaato.switchgear.circuitbreaker.state.Strategy;

public class BucketedStateManagersHolderTest {

    private static final int MAX_CONSECUTIVE_FAILURE_ALLOWED = 5;
    private static final int CIRCUIT_OPEN_TIME_IN_MILLIS = 10;
    private static final BucketRange RANGE = new BucketRange(0, 10);
    private static final BucketRange FIRST_RANGE = new BucketRange(10, 20);
    private static final BucketRange SECOND_RANGE = new BucketRange(20, 30);

    private final BucketedStateManagersHolder bucketedStateManagersHolder = new BucketedStateManagersHolder(
            Configuration.builder()
                         .withCircuitOpenTimeInMillis(CIRCUIT_OPEN_TIME_IN_MILLIS)
                         .withMaxConsecutiveFailuresAllowed(MAX_CONSECUTIVE_FAILURE_ALLOWED)
                         .build()
    );

    @Test
    public void whenCallingWithTheSameTimeoutRangeThenReturnSameStateManager() {
        final StateManager firstManager = bucketedStateManagersHolder.getStateManager(FIRST_RANGE);
        final StateManager secondManager = bucketedStateManagersHolder.getStateManager(FIRST_RANGE);
        assertThat(secondManager, is(firstManager));
    }

    @Test
    public void whenCallingWithDifferentTimeoutRangesThenReturnDifferentStateManager() {
        final StateManager firstManager = bucketedStateManagersHolder.getStateManager(FIRST_RANGE);
        final StateManager secondManager = bucketedStateManagersHolder.getStateManager(SECOND_RANGE);
        assertThat(secondManager, not(firstManager));
    }

    @Test
    public void whenDefaultValueForStrategyThenReturnConsecutiveFailuresStateManager() {
        final BucketedStateManagersHolder defaultStateManagerHolder = new BucketedStateManagersHolder(
                Configuration.builder().build()
        );
        final StateManager stateManager = defaultStateManagerHolder.getStateManager(RANGE);
        assertTrue(stateManager instanceof ConsecutiveFailuresStateManager);
    }

    @Test
    public void whenFrequentFailuresStrategyThenReturnFrequentFailuresStateManager() {
        final BucketedStateManagersHolder defaultStateManagerHolder = new BucketedStateManagersHolder(
                Configuration.builder()
                             .withStateManagerStrategy(Strategy.FREQUENT_FAILURES)
                             .build()
        );
        final StateManager stateManager = defaultStateManagerHolder.getStateManager(RANGE);
        assertTrue(stateManager instanceof FrequentFailuresStateManager);
    }

    @Test
    public void whenConsecutiveFailuresStrategyThenReturnConsecutiveFailuresStateManager() {
        final BucketedStateManagersHolder defaultStateManagerHolder = new BucketedStateManagersHolder(
                Configuration.builder()
                             .withStateManagerStrategy(Strategy.CONSECUTIVE_FAILURES)
                             .build()
        );
        final StateManager stateManager = defaultStateManagerHolder.getStateManager(RANGE);
        assertTrue(stateManager instanceof ConsecutiveFailuresStateManager);
    }
}