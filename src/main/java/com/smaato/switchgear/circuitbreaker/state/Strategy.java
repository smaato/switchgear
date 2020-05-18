package com.smaato.switchgear.circuitbreaker.state;

public enum Strategy {

    /**
     * With this strategy Circuit Breaker is incrementing consecutive failures counter and
     * when the counter exceeds the threshold the following calls are blocked. <br>
     * Successful calls resets the failure counter back to zero.
     */
    CONSECUTIVE_FAILURES,

    /**
     * <p>
     * With this strategy Circuit Breaker is looking into the percentage of failed calls in a given
     * period of time and if the percentage of failures is exceeding the defined threshold
     * the following calls will be throttled. <br>
     * If the acceptable ratio of successful calls will be restored, then throttling will be disabled.
     * </p>
     * <p>
     * In a case when the evaluated time frame had 100% ratio of failed calls, then Circuit Breaker will open circuit and
     * block following calls. After the given half open time the trial request will be sent.<br>
     * Successful trial call will close the circuit.
     * </p>
     */
    FREQUENT_FAILURES
}
