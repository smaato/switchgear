package com.smaato.switchgear.circuitbreaker;

public class CircuitBreakerOpenException extends RuntimeException {

    private static final long serialVersionUID = 8558425857113015728L;

    public CircuitBreakerOpenException(final Throwable cause) {
        super(cause);
    }
}
