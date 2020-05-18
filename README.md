# Switchgear

![build](https://github.com/smaato/switchgear/workflows/build/badge.svg?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

In Smaato, we are processing vast volumes of outgoing HTTP requests while performing [OpenRTB](https://iabtechlab.com/standards/openrtb/) auctions. In case when one of our partners has an outage, the whole auction is waiting for the timeout. To achieve resiliency and fault tolerance, we've developed Switchgear.

Switchgear is a Java library that provides an API for call isolation, timeouts, and circuit breaker functionality. Our main goal is to achieve minimal performance overhead of the library, even when used in the hottest place of your application. Additionally, we've opted for zero transitive dependencies and maintaining a fluent API.

## Communication
The project is developed and maintained by the Smaato engineering team. You can reach us at <betelgeuse@smaato.com>.

## Contributing
Please review our [contribution guidelines](https://github.com/smaato/switchgear/blob/master/CONTRIBUTING.md).

## Java Version
You will need Java 8 or later.

## Hello World!
```java
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.Switchgear;
import com.smaato.switchgear.model.Action;
import com.smaato.switchgear.model.Outcome;

public class HelloWorld {

    private static final Configuration configuration = Configuration.builder().build();

    private static final Switchgear switchgear = Switchgear.newInstance(configuration);

    public static void main(final String[] args) {

        final Action<String> action = Action.from(() -> "Hello World!");

        final Outcome<String> outcome = switchgear.execute(action);

        outcome.getValue()
               .ifPresent(System.out::print);
    }
}
```

## Hello Fallback!
```java
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Callable;

import com.smaato.switchgear.Configuration;
import com.smaato.switchgear.Switchgear;
import com.smaato.switchgear.model.Action;

public class HelloFallback {

    private static final int ONE_SECOND = (int) SECONDS.toMillis(1);
    private static final int FIVE_SECONDS = (int) SECONDS.toMillis(5);
    private static final int ONE_FAILURE = 1;

    private static final Configuration configuration = Configuration.builder()
                                                                    .withDefaultTimeoutInMillis(ONE_SECOND)
                                                                    .withCircuitOpenTimeInMillis(FIVE_SECONDS)
                                                                    .withMaxConsecutiveFailuresAllowed(ONE_FAILURE)
                                                                    .build();

    private static final Switchgear switchgear = Switchgear.newInstance(configuration);

    public static void main(final String[] args) {

        final Callable<String> sleepyCall = () -> {
            sleep(FIVE_SECONDS);
            return "Hello World!";
        };

        final Action<String> action = Action.builder(sleepyCall)
                                            .withCircuitBreakerFallback(failure -> "Hello Fallback!")
                                            .build();

        // Failing execution
        switchgear.execute(action)
                  .getValue()
                  .ifPresent(System.out::print);

        // Fallback execution
        switchgear.execute(action)
                  .getValue()
                  .ifPresent(System.out::print);
    }
}
```
