package com.smaato.switchgear.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;

@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public class ThreadFactoryBuilderTest {

    private static final String DEFAULT_FIRST_THREAD_NAME = "pool\\-\\d+\\-thread\\-1";
    private static final String NEW_FIRST_THREAD_NAME = "new-name-1";
    private static final String NEW_NAME = "new-name";

    private final Runnable runnableMock = mock(Runnable.class);

    private final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();

    @Test
    public void whenDefaultThreadFactoryBuiltThenThreadHasDefaultValues() {
        final ThreadFactory factory = threadFactoryBuilder.build();

        final Thread thread = factory.newThread(runnableMock);

        assertTrue(thread.getName().matches(DEFAULT_FIRST_THREAD_NAME));
        assertThat(thread.getPriority(), is(Thread.NORM_PRIORITY));
        assertFalse(thread.isDaemon());
    }

    @Test
    public void whenSetDaemonThenThreadIsDaemon() {
        final ThreadFactory factory = threadFactoryBuilder.isDaemon().build();

        final Thread thread = factory.newThread(runnableMock);

        assertTrue(thread.isDaemon());
    }

    @Test
    public void whenSetNameThenThreadHasNonDefaultName() {
        final ThreadFactory factory = threadFactoryBuilder.withName(NEW_NAME).build();

        final Thread thread = factory.newThread(runnableMock);

        assertThat(thread.getName(), is(NEW_FIRST_THREAD_NAME));
    }
}