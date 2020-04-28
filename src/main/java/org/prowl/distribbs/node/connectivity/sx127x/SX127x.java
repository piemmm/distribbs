package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;

/**
 * Implements an interface using the SX127x sx1276, sx1278, etc) series of chips
 * which support several different modulations including LoRa, GMSK, GFSK, MSK,
 * FSK(rtty etc), OOK(cw-ish), as well as being capable of being directly driven
 * to tx several other modulation types
 * 
 * Data to send is compressed before it is sent, and decompressed in any rx
 * events when required.
 */
public class SX127x implements Connector {

   private static final Log          LOG  = LogFactory.getLog("SX127x");

   private HierarchicalConfiguration config;

   private Device                    device;

   /**
    * Should we announce?
    */
   private boolean                   announce;

   /**
    * Announce period in minutes if no tx activity from us(absolute minimum 15
    * minutes)
    */
   private int                       announcePeriod;

   /**
    * Our modulation mode
    */
   private Modulation                modulation;

   /**
    * The slot we are controlling Slot 0 = 144MHz, Slot 1 = 433MHz
    */
   private int                       slot = 0;

   /**
    * Our state holder for connections
    * 
    * @param config
    */
   private PacketEngine              packetEngine;
   
   private long txCompressedByteCount = 1;
   private long txUncompressedByteCount = 1;
   private long rxCompressedByteCount = 1;
   private long rxUncompressedByteCount = 1;

   public SX127x(HierarchicalConfiguration config) {
      this.config = config;
   }

   public void start() throws IOException {
      slot = config.getInt("slot", 0);
      int frequency = config.getInt("frequency", 0);
      int deviation = config.getInt("deviation", 2600);
      int baud = config.getInt("baud",4800);

      announce = config.getBoolean("announce");
      announcePeriod = config.getInt("announcePeriod");
      modulation = Modulation.findByName(config.getString("modulation", Modulation.MSK.name()));

      // Perform validation of config

      packetEngine = new PacketEngine(this);
      if (Modulation.LoRa.equals(modulation)) {
         device = new LoRaDevice(this, slot, frequency, deviation, baud);
      } else if (Modulation.MSK.equals(modulation)) {
         device = new MSKDevice(this, slot, frequency, deviation, baud);
      } else {
         // Not a known modulation.
         throw new IOException("Unknown modulation:" + config.getString("modulation"));
      }

   }

   public void stop() {

   }

   public boolean isAnnounce() {
      return announce;
   }

   public int getAnnouncePeriod() {
      return announcePeriod;
   }

   public Modulation getModulation() {
      return modulation;
   }

   public PacketEngine getPacketEngine() {
      return packetEngine;
   }

   public boolean isRF() {
      return true;
   }

   @Override
   public boolean canSend() {
      return true;
   }

   @Override
   public boolean sendPacket(TxRFPacket packet) {
      packet.setConnector(this);
      if (device == null || packet == null)
         return false;

      device.sendMessage(packet);
      return true;
   }

   public String getName() {
      return getClass().getSimpleName();
   }

   public int getFrequency() {
      if (device == null) {
         return 0;
      }
      return device.getFrequency();
   }

   public double getNoiseFloor() {
      if (device == null) {
         return 0;
      }
      return device.getNoiseFloor();
   }

   public double getRSSI() {
      if (device == null) {
         return 0;
      }
      return device.getRSSI();
   }

   @Override
   public int getSlot() {
      return slot;
   }

   @Override
   public long getTxCompressedByteCount() {
      return txCompressedByteCount;
   }

   @Override
   public long getTxUncompressedByteCount() {
      // TODO Auto-generated method stub
      return txUncompressedByteCount;
   }

   @Override
   public long getRxCompressedByteCount() {
      // TODO Auto-generated method stub
      return rxCompressedByteCount;
   }

   @Override
   public long getRxUncompressedByteCount() {
      // TODO Auto-generated method stub
      return rxUncompressedByteCount;
   }
   
   protected void addRxStats(long compressedByteCount, long uncompressedByteCount) {
      rxCompressedByteCount+= compressedByteCount;
      rxUncompressedByteCount = uncompressedByteCount;
   }
  
   protected void addTxStats(long compressedByteCount, long uncompressedByteCount) {
      txCompressedByteCount+= compressedByteCount;
      txUncompressedByteCount = uncompressedByteCount;
   }
   

}
