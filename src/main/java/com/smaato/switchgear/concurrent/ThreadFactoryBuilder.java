package com.smaato.switchgear.concurrent;

import static java.lang.String.format;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadFactoryBuilder {

    private static final int INITIAL_THREAD_NUMBER = 1;
    private static final String THREAD_COUNTER_FORMAT = "-%d";

    private String nameFormat;
    private Boolean daemon;

    public static ThreadFactoryBuilder builder() {
        return new ThreadFactoryBuilder();
    }

    public ThreadFactoryBuilder withName(final String name) {
        nameFormat = name + THREAD_COUNTER_FORMAT;
        return this;
    }

    public ThreadFactoryBuilder isDaemon() {
        daemon = true;
        return this;
    }

    public ThreadFactory build() {
        final AtomicLong count = new AtomicLong(INITIAL_THREAD_NUMBER);
        return runnable -> {
            final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            if (nameFormat != null) {
                thread.setName(format(nameFormat, count.getAndIncrement()));
            }
            if (daemon != null) {
                thread.setDaemon(daemon);
            }
            return thread;
        };
    }
}
