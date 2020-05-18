package com.smaato.switchgear.circuitbreaker.state.bucket;

import java.util.Random;
import java.util.function.Supplier;

public class ThrottlingUtil {
    private final int throttlingPercentage;
    private final Supplier<Random> randomSupplier;

    ThrottlingUtil(final int throttlingPercentage,
                   final Supplier<Random> randomSupplier) {
        this.throttlingPercentage = throttlingPercentage;
        this.randomSupplier = randomSupplier;
    }

    public boolean throttle() {
        return randomSupplier.get().nextInt(100) < throttlingPercentage;
    }
}
