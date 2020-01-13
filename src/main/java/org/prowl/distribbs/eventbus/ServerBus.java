package org.prowl.distribbs.eventbus;

import org.prowl.distribbs.eventbus.events.BaseEvent;

import com.google.common.eventbus.EventBus;

/**
 * Any customisations to event firing can go in here.
 */
public enum ServerBus {

   INSTANCE;

   private EventBus eventBus;

   ServerBus() {
      eventBus = new EventBus();
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