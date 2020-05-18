package com.smaato.switchgear.circuitbreaker.state.bucket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.circuitbreaker.state.ConsecutiveFailuresStateManager;
import com.smaato.switchgear.circuitbreaker.state.FrequentFailuresStateManager;
import com.smaato.switchgear.circuitbreaker.state.StateManager;
import com.smaato.switchgear.circuitbreaker.state.Strategy;

public class BucketedStateManagersHolder {

    private final Map<BucketRange, StateManager> stateManagers = new ConcurrentHashMap<>();

    private final Configuration configuration;

    public BucketedStateManagersHolder(final Configuration configuration) {
        this.configuration = configuration;
    }

    public StateManager getStateManager(final BucketRange timeoutBucketRange) {
        return stateManagers.computeIfAbsent(timeoutBucketRange, range -> getStateManager());
    }

    private StateManager getStateManager() {
        if (configuration.getStateManagerStrategy() == Strategy.FREQUENT_FAILURES) {
            final ThrottlingUtil throttlingUtil = new ThrottlingUtil(configuration.getThrottlingPercentage(), ThreadLocalRandom::current);
            return FrequentFailuresStateManager.newInstance(configuration.getAcceptableFailuresPercentage(),
                                                            configuration.getCircuitOpenTimeInMillis(),
                                                            configuration.getMinimumWindowSize(),
                                                            throttlingUtil,
                                                            configuration.getScheduledExecutor());
        } else {
            return ConsecutiveFailuresStateManager.newInstance(configuration.getMaxConsecutiveFailuresAllowed(),
                                                               configuration.getCircuitOpenTimeInMillis(),
                                                               configuration.getScheduledExecutor());
        }
    }
}
