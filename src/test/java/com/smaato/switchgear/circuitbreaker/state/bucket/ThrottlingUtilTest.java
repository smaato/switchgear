package com.smaato.switchgear.circuitbreaker.state.bucket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Test;

public class ThrottlingUtilTest {
    private static final int THROTTLING_PERCENTAGE = 50;
    private static final int HUNDRED_PERCENT_BOUND = 100;
    private static final int BELOW_THRESHOLD = 45;
    private static final int ABOVE_THRESHOLD = 55;

    private final Random randomMock = mock(Random.class);

    private final ThrottlingUtil throttlingUtil = new ThrottlingUtil(THROTTLING_PERCENTAGE, () -> randomMock);

    @Test
    public void whenRandomBelowThresholdThenThrottle() {
        when(randomMock.nextInt(HUNDRED_PERCENT_BOUND)).thenReturn(BELOW_THRESHOLD);

        assertTrue(throttlingUtil.throttle());
    }

    @Test
    public void whenRandomIsEqualToThresholdThenDoNotThrottle() {
        when(randomMock.nextInt(HUNDRED_PERCENT_BOUND)).thenReturn(THROTTLING_PERCENTAGE);

        assertFalse(throttlingUtil.throttle());
    }

    @Test
    public void whenRandomAboveThresholdThenDoNotThrottle() {
        when(randomMock.nextInt(HUNDRED_PERCENT_BOUND)).thenReturn(ABOVE_THRESHOLD);

        assertFalse(throttlingUtil.throttle());
    }

    @Test
    public void whenThresholdIsZeroThenNeverThrottle() {
        final Random random = new Random();
        final ThrottlingUtil localThrottlingUtil = new ThrottlingUtil(0, () -> random);

        for(int i =0; i< 100; i++) {
            assertFalse(localThrottlingUtil.throttle());
        }
    }

    @Test
    public void whenThresholdIsOneHundredThenAlwaysThrottle() {
        final Random random = new Random();
        final ThrottlingUtil localThrottlingUtil = new ThrottlingUtil(100, () -> random);

        for(int i =0; i< 100; i++) {
            assertTrue(localThrottlingUtil.throttle());
        }
    }

    @Test
    public void whenThresholdIsBelowZeroThenNeverThrottle() {
        final Random random = new Random();
        final ThrottlingUtil localThrottlingUtil = new ThrottlingUtil(-1, () -> random);

        for(int i =0; i< 100; i++) {
            assertFalse(localThrottlingUtil.throttle());
        }
    }

    @Test
    public void whenThresholdIsAboveOneHundredThenAlwaysThrottle() {
        final Random random = new Random();
        final ThrottlingUtil localThrottlingUtil = new ThrottlingUtil(101, () -> random);

        for(int i =0; i< 100; i++) {
            assertTrue(localThrottlingUtil.throttle());
        }
    }
}