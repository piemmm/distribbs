package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;
import org.prowl.distribbs.utils.Tools;

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

   private static final Log          LOG = LogFactory.getLog("SX127x");

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
    * Our state holder for connections
    * @param config
    */
   private PacketEngine packetEngine;
   

   public SX127x(HierarchicalConfiguration config) {
      this.config = config;

   }

   public void start() throws IOException {
      announce = config.getBoolean("announce");
      announcePeriod = config.getInt("announcePeriod");
      modulation = Modulation.findByName(config.getString("modulation", Modulation.MSK.name()));
      packetEngine = new PacketEngine(this);
      if (Modulation.LoRa.equals(modulation)) {
         device = new LoRaDevice(this);
      } else if (Modulation.MSK.equals(modulation)) {
         device = new MSKDevice(this);
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
}
