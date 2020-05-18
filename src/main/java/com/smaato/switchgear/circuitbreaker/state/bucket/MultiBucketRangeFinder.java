package com.smaato.switchgear.circuitbreaker.state.bucket;

public class MultiBucketRangeFinder implements BucketRangeFinder {

    private final int bucketSizeInMillis;

    public MultiBucketRangeFinder(final int bucketSizeInMillis) {
        if (bucketSizeInMillis <= 0) {
            throw new IllegalArgumentException("Bucket size should be greater then zero");
        }
        this.bucketSizeInMillis = bucketSizeInMillis;
    }

    @Override
    public BucketRange find(final int timeout) {
        final int lowerBound = (timeout / bucketSizeInMillis) * bucketSizeInMillis;
        final int upperBound = (lowerBound + bucketSizeInMillis) - 1;
        return new BucketRange(lowerBound, upperBound);
    }
}
