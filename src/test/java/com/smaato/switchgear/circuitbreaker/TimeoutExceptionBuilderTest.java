package com.smaato.switchgear.circuitbreaker;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.smaato.switchgear.circuitbreaker.state.bucket.BucketRange;

public class TimeoutExceptionBuilderTest {

    @Test
    public void whenFiniteTimeoutRangeThenIncludeThatRangeInErrorMessage() {
        final BucketRange finiteRange = new BucketRange(50, 100);

        final TimeoutException timeoutException = TimeoutExceptionBuilder.builder().withTimeoutRange(finiteRange).build();

        assertThat(timeoutException.getMessage(), is("Circuit breaker timeout for timeout bucket 50-100 ms"));
    }

    @Test
    public void whenInfiniteTimeoutRangeThenDoNotIncludeThatRangeInErrorMessage() {
        final TimeoutException timeoutException = TimeoutExceptionBuilder.builder()
                                                                         .withTimeoutRange(new BucketRange(Integer.MIN_VALUE,
                                                                                                           Integer.MAX_VALUE))
                                                                         .build();

        assertThat(timeoutException.getMessage(), is("Circuit breaker timeout "));
    }
}