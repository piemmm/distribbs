package org.prowl.distribbs.node.connectivity.ipv4.events;

import org.prowl.distribbs.eventbus.events.BaseEvent;

/**
 * Fired when a node via an IP connects and has authenticated successfully
 * 
 * Used to clear off any older clients that may be in a zombie state.
 * 
 * @param callsign The node callsign.
 */
public class IPNodeConnectedEvent extends BaseEvent {

   private String callsign;

   public IPNodeConnectedEvent(String callsign) {
      this.callsign = callsign;
   }

   public String getCallsign() {
      return callsign;
   }

}
