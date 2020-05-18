package com.smaato.switchgear.circuitbreaker.state;

public interface StateManager {

    boolean isOpen();

    void handleSuccess();

    void handleFailure();
}
