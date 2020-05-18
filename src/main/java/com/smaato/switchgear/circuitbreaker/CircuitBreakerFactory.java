package com.smaato.switchgear.circuitbreaker;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRangeFinder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedFailureStatesHolder;
import com.smaato.switchgear.circuitbreaker.state.bucket.BucketedStateManagersHolder;
import com.smaato.switchgear.circuitbreaker.state.bucket.MultiBucketRangeFinder;
import com.smaato.switchgear.circuitbreaker.state.bucket.SingleBucketRangeFinder;

public class CircuitBreakerFactory {

    public static CircuitBreaker newInstance(final Configuration configuration) {
        final BucketedStateManagersHolder stateManagersHolder = new BucketedStateManagersHolder(configuration);
        final BucketedFailureStatesHolder failureStatesHolder = new BucketedFailureStatesHolder();

        final BucketRangeFinder bucketRangeFinder = getBucketRangeFinder(configuration.getBucketLengthInMillis());

        return new CircuitBreakerImpl(stateManagersHolder,
                                      new TimeoutScheduler(configuration.getScheduledExecutor()),
                                      failureStatesHolder,
                                      bucketRangeFinder,
                                      configuration.getRecognizedExceptions());
    }

    private static BucketRangeFinder getBucketRangeFinder(final Integer bucketLengthInMillis) {
        if (bucketLengthInMillis == null) {
            return SingleBucketRangeFinder.INSTANCE;
        }
        return new MultiBucketRangeFinder(bucketLengthInMillis);
    }
}