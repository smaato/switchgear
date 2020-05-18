package com.smaato.switchgear.circuitbreaker.state.bucket;

import java.util.Objects;

public class BucketRange {

    private final int lowerBound;
    private final int upperBound;

    public BucketRange(final int lowerBound,
                       final int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final BucketRange that = (BucketRange) o;
        return (lowerBound == that.lowerBound) &&
                (upperBound == that.upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound);
    }

    @Override
    public String toString() {
        return lowerBound + "-" + upperBound;
    }
}
