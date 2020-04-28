package org.prowl.distribbs.node.connectivity;

import java.io.IOException;

import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;

public interface Connector {

   public void start() throws IOException;

   public void stop();

   public String getName();
    
   public boolean isAnnounce();

   public int getAnnouncePeriod();

   public Modulation getModulation();
   
   public PacketEngine getPacketEngine();

   public boolean isRF();
   
   public boolean canSend();
   
   public boolean sendPacket(TxRFPacket packet);
   
   public int getFrequency();
   
   public double getNoiseFloor();
   
   public double getRSSI();
   
   /**
    * Get the physical hardware 'slot' this RF interface occupies
    * @return
    */
   public int getSlot();
   
   public long getTxCompressedByteCount();
   public long getTxUncompressedByteCount();
   public long getRxCompressedByteCount();
   public long getRxUncompressedByteCount();

   
}
