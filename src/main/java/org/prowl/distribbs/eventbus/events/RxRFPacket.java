package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.node.connectivity.Connector;

/**
 * An event fired when one of the RF interfaces receives a packet (which may ba valid, invalid, etc)
 */
public class RxRFPacket extends BaseEvent {

   private long rxTime;
   private byte[] packet;
   private Connector connector;

   public RxRFPacket(Connector connector, byte[] packet, long rxTime) {
      super();
      this.rxTime = rxTime;
      this.connector = connector;
      this.packet = packet;
   }

   public byte[] getPacket() {
      return packet;
   }

   public Connector getConnector() {
      return connector;
   }
   
   public long getRxTime() {
      return rxTime;
   }
}
