package com.smaato.switchgear.circuitbreaker;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class CircuitBreakerHolderTest {

    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";

    private final CircuitBreakerHolder circuitBreakerHolder = new CircuitBreakerHolder(() -> mock(CircuitBreaker.class));

    @Test
    public void whenSameGroupThenSameCircuitBreaker() {
        final CircuitBreaker firstCircuitBreaker = circuitBreakerHolder.getFor(GROUP_1);
        final CircuitBreaker secondCircuitBreaker = circuitBreakerHolder.getFor(GROUP_1);

        assertThat(firstCircuitBreaker, is(secondCircuitBreaker));
    }

    @Test
    public void whenDifferentGroupsThenDifferentCircuitBreakers() {
        final CircuitBreaker firstCircuitBreaker = circuitBreakerHolder.getFor(GROUP_1);
        final CircuitBreaker secondCircuitBreaker = circuitBreakerHolder.getFor(GROUP_2);

        assertThat(firstCircuitBreaker, is(not(secondCircuitBreaker)));
    }

    @Test(expected = NullPointerException.class)
    public void whenRequestedWithNullThenThrowException() {
        circuitBreakerHolder.getFor(null);
    }
}