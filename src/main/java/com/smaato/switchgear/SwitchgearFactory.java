package com.smaato.switchgear;

import java.util.function.Supplier;

import com.smaato.switchgear.circuitbreaker.CircuitBreaker;
import com.smaato.switchgear.circuitbreaker.CircuitBreakerFactory;
import com.smaato.switchgear.circuitbreaker.CircuitBreakerHolder;
import com.smaato.switchgear.circuitbreaker.DummyCircuitBreaker;
import com.smaato.switchgear.concurrent.ExceptionUnwrapper;

enum SwitchgearFactory {

    INSTANCE;

    Switchgear createFrom(final Configuration configuration) {
        final CircuitBreakerHolder circuitBreakerHolder = new CircuitBreakerHolder(getCircuitBreakerSupplier(configuration));
        final ExecutorService executorService = new ExecutorService(configuration.getExecutor(), circuitBreakerHolder);
        final ResultCollector resultCollector = new ResultCollector(ExceptionUnwrapper.INSTANCE);

        return new Switchgear(executorService,
                              resultCollector,
                              configuration.getTimeoutInMillis());
    }

    private static Supplier<CircuitBreaker> getCircuitBreakerSupplier(final Configuration configuration) {
        if (configuration.isCircuitBreakerEnabled()) {
            return () -> CircuitBreakerFactory.newInstance(configuration);
        }
        return DummyCircuitBreaker::new;
    }
}
