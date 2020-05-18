package com.smaato.switchgear.concurrent;

class SwitchgearRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -8196915313065337499L;

    SwitchgearRuntimeException(final Exception e) {
        super(e);
    }
}
