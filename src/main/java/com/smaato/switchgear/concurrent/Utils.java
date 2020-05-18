package com.smaato.switchgear.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Utils {

    private static final Utils SINGLETON = new Utils();

    private Utils() {
    }

    public static Utils singleton() {
        return SINGLETON;
    }

    public <T> Supplier<T> convertToSupplier(final Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (final Exception e) {
                throw new SwitchgearRuntimeException(e);
            }
        };
    }
}
