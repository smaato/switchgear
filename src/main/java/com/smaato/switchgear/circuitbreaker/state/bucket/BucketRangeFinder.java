package com.smaato.switchgear.circuitbreaker.state.bucket;

public interface BucketRangeFinder {
    BucketRange find(final int timeout);
}
