package com.smaato.switchgear.circuitbreaker;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CircuitBreakerHolder {

    private static final int INITIAL_CAPACITY = 1;

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    private final Supplier<CircuitBreaker> circuitBreakerSupplier;

    public CircuitBreakerHolder(final Supplier<CircuitBreaker> circuitBreakerSupplier) {
        this.circuitBreakerSupplier = circuitBreakerSupplier;
    }

    public CircuitBreaker getFor(final String groupName) {
        Objects.requireNonNull(groupName);

        return circuitBreakers.computeIfAbsent(groupName, gn -> circuitBreakerSupplier.get());
    }
}
