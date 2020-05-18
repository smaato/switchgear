package com.smaato.switchgear.circuitbreaker.state.bucket;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BucketRangeTest {

    @Test
    public void whenSameBucketRangeThenInstancesShouldBeEqual() {
        final BucketRange firstRange = new BucketRange(30, 40);
        final BucketRange secondRange = new BucketRange(30, 40);

        assertThat(firstRange, is(secondRange));
    }

    @Test
    public void whenDifferentBucketRangeThenInstancesShouldNotBeEqual() {
        final BucketRange firstRange = new BucketRange(30, 40);
        final BucketRange secondRange = new BucketRange(40, 50);

        assertThat(firstRange, not(secondRange));
    }
}