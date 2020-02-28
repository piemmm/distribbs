package org.prowl.distribbs.node.connectivity.sx127x;

import org.prowl.distribbs.eventbus.events.TxRFPacket;

public interface Device {

   public void sendMessage(TxRFPacket packet);

   public int getFrequency();
   
   public double getNoiseFloor();

   public double getRSSI();
}
