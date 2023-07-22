package org.prowl.distribbs.eventbus;

import com.google.common.eventbus.AsyncEventBus;
import org.prowl.distribbs.eventbus.events.BaseEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Any customisations to event firing can go in here.
 */
public enum ServerBus {

    INSTANCE;

    private AsyncEventBus eventBus;
    private ExecutorService pool = Executors.newFixedThreadPool(2);

    ServerBus() {
        eventBus = new AsyncEventBus(Executors.newFixedThreadPool(5));
    }

    public final void post(final BaseEvent event) {
        // Async still waits for all subscribers to run before returning,
        // but we don't care about waiting so it gets launched in it's own thread
        pool.execute(new Runnable() {
            public final void run() {
                eventBus.post(event);
            }
        });
    }

    public final void register(final Object o) {
        eventBus.register(o);
    }

    public final void unregister(final Object o) {
        eventBus.unregister(o);
    }

}