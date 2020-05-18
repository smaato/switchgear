package com.smaato.switchgear.circuitbreaker;

import static com.smaato.switchgear.circuitbreaker.state.bucket.SingleBucketRangeFinder.INCLUDE_ALL_BUCKET;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;

class TimeoutExceptionBuilder {

    private static final String CIRCUIT_BREAKER_TIMEOUT_ERROR = "Circuit breaker timeout %s";
    private BucketRange timeoutRange;

    static TimeoutExceptionBuilder builder() {
        return new TimeoutExceptionBuilder();
    }

    TimeoutExceptionBuilder withTimeoutRange(final BucketRange timeoutRange) {
        this.timeoutRange = timeoutRange;
        return this;
    }

    TimeoutException build() {
        final String trailingErrorMessage = (!Objects.equals(INCLUDE_ALL_BUCKET, timeoutRange)) ? String.format("for timeout bucket %s ms",
                                                                                                                timeoutRange) : "";

        return new TimeoutException(String.format(CIRCUIT_BREAKER_TIMEOUT_ERROR, trailingErrorMessage));
    }
}
