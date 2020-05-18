package com.smaato.switchgear.circuitbreaker.state.bucket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smaato.switchgear.circuitbreaker.state.LastFailureCause;

public class BucketedFailureStatesHolder {

    private final Map<BucketRange, LastFailureCause> failureStates = new ConcurrentHashMap<>();

    public LastFailureCause getFailureState(final BucketRange timeoutBucketRange) {
        return failureStates.computeIfAbsent(timeoutBucketRange, ignored -> new LastFailureCause());
    }
}
