package org.prowl.distribbs.eventbus;

import java.util.concurrent.Executors;

import org.prowl.distribbs.eventbus.events.BaseEvent;

import com.google.common.eventbus.AsyncEventBus;

/**
 * Any customisations to event firing can go in here.
 */
public enum ServerBus {

   INSTANCE;

   private AsyncEventBus eventBus;

   ServerBus() {
      eventBus = new AsyncEventBus(Executors.newFixedThreadPool(5));
   }

   public void post(BaseEvent event) {
      eventBus.post(event);
   }

   public void register(Object o) {
      eventBus.register(o);
   }

   public void unregister(Object o) {
      eventBus.unregister(o);
   }

   public void post(Object o) {
      eventBus.post(o);
   }

}