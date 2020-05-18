package com.smaato.switchgear;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

public class Switchgear {

    private final ExecutorService executorService;
    private final ResultCollector resultCollector;
    private final int defaultTimeoutInMillis;

    Switchgear(final ExecutorService executorService,
               final ResultCollector resultCollector,
               final int defaultTimeoutInMillis) {
        this.executorService = executorService;
        this.resultCollector = resultCollector;
        this.defaultTimeoutInMillis = defaultTimeoutInMillis;
    }

    /**
     * Creates new instance of Switchgear with the given configuration.
     *
     * @param configuration provide non-null Switchgear configuration
     * @throws NullPointerException if configuration is null.
     */
    public static Switchgear newInstance(final Configuration configuration) {
        requireNonNull(configuration);
        return SwitchgearFactory.INSTANCE.createFrom(configuration);
    }

    /**
     * <p>Execute {@link Action#call} in a separate thread. Execution takes not more then timeout set for the {@link Action}.</p>
     *
     * @param action {@link Action} for isolated execution
     * @return {@link Outcome} with the result of execution or failure
     * @throws NullPointerException if action is null.
     */
    public <T> Outcome<T> execute(final Action<T> action) {
        requireNonNull(action);

        final int timeoutInMillis = getTimeoutInMillis(action);
        final Future<T> future = executorService.execute(action, timeoutInMillis);
        return resultCollector.getOutcome(action, future, timeoutInMillis);
    }

    /**
     * <p>Execute all provided actions in parallel. Every {@link Action} is using its timeout to limit the execution time.</p>
     * <p>Actions with the same group name will share the same circuit breaker instance. Every execution may individually be
     * a subject to a fallback behaviour due to a open circuit state of respective circuit breaker.</p>
     *
     * @param actions collection of {@link Action}s for parallel execution
     * @throws NullPointerException if actions collection is null or any individual action is null
     */
    public <T> Collection<Outcome<T>> executeInParallel(final Collection<Action<T>> actions) {
        requireNonNull(actions);

        final Map<Action<T>, Future<T>> futures = new HashMap<>(actions.size());
        for (final Action<T> action : actions) {
            final int timeoutInMillis = getTimeoutInMillis(action);
            futures.put(action, executorService.execute(action, timeoutInMillis));
        }

        final Collection<Outcome<T>> outcomes = new ArrayList<>(actions.size());
        for (final Action<T> action : actions) {
            final Future<T> future = futures.get(action);
            final int timeoutInMillis = getTimeoutInMillis(action);
            final Outcome<T> outcome = resultCollector.getOutcome(action, future, timeoutInMillis);
            outcomes.add(outcome);
        }
        return outcomes;
    }

    private <T> Integer getTimeoutInMillis(final Action<T> action) {
        return action.getTimeoutInMillis().orElse(defaultTimeoutInMillis);
    }
}
