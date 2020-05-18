package com.smaato.switchgear.circuitbreaker.state.bucket;

public enum  SingleBucketRangeFinder implements BucketRangeFinder {

    INSTANCE;

    public static final BucketRange INCLUDE_ALL_BUCKET = new BucketRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

    @Override
    public BucketRange find(final int timeout) {
        return INCLUDE_ALL_BUCKET;
    }
}
