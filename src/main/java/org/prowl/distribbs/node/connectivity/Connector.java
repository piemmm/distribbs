package org.prowl.distribbs.node.connectivity;

import java.io.IOException;

import org.prowl.distribbs.core.PacketEngine;

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
   
   public boolean sendPacket(byte[] data);
   
}
