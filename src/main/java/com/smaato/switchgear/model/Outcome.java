package com.smaato.switchgear.model;

import java.util.Optional;

public class Outcome<T> {

    private final Throwable failure;
    private final T value;

    public Outcome(final T value) {
        this.value = value;
        failure = null;
    }

    public Outcome(final Throwable failure) {
        value = null;
        this.failure = failure;
    }

    public Optional<Throwable> getFailure() {
        return Optional.ofNullable(failure);
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }
}
