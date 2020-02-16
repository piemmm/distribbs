package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.node.connectivity.Connector;

public class TxRFPacket extends BaseEvent {

   private byte[] packet;
   private Connector connector;

   public TxRFPacket(Connector connector, byte[] packet) {
      super();
      this.connector = connector;
      this.packet = packet;
   }

   public byte[] getPacket() {
      return packet;
   }

   public Connector getConnector() {
      return connector;
   }
   
   
}
