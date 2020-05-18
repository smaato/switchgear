package com.smaato.switchgear.circuitbreaker.state.bucket;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MultiBucketRangeFinderTest {
    private static final int RANGE_SIZE = 10;

    private final BucketRangeFinder keyFinder = new MultiBucketRangeFinder(RANGE_SIZE);

    @Test
    public void whenNumbersInSameRangeThenReturnEqualRange() {
        final int firstTimeout = 17;
        final int secondTimeout = 18;

        final BucketRange firstRange = keyFinder.find(firstTimeout);
        final BucketRange secondRange = keyFinder.find(secondTimeout);

        assertThat(secondRange, is(firstRange));
    }

    @Test
    public void whenNumbersInDifferentRangeThenReturnDifferent() {
        final int firstTimeout = 17;
        final int secondTimeout = 21;

        final BucketRange firstRange = keyFinder.find(firstTimeout);
        final BucketRange secondRange = keyFinder.find(secondTimeout);

        assertThat(secondRange, not(firstRange));
    }

    @Test
    public void testLowerBoundIsIncludedInRange() {
        final int firstTimeout = 17;
        final int secondTimeout = 10;

        final BucketRange firstRange = keyFinder.find(firstTimeout);
        final BucketRange secondRange = keyFinder.find(secondTimeout);

        assertThat(secondRange, is(firstRange));
    }

    @Test
    public void testUpperBoundIsNotIncludedInRange() {
        final int firstTimeout = 17;
        final int secondTimeout = 20;

        final BucketRange firstRange = keyFinder.find(firstTimeout);
        final BucketRange secondRange = keyFinder.find(secondTimeout);

        assertThat(secondRange, not(firstRange));
    }
}