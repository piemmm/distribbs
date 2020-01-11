package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

import com.pi4j.io.gpio.GpioFactory;

/**
 * Implements an interface using the SX127x sx1276, sx1278, etc) series of chips
 * which support several different modulations including LoRa, GMSK, GFSK, MSK,
 * FSK(rtty etc), OOK(cw-ish), as well as being capable of being directly driven
 * to tx several other modulation types
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

   public SX127x(HierarchicalConfiguration config) {
      this.config = config;

   }

   public void start() throws IOException {
      announce = config.getBoolean("announce");
      announcePeriod = config.getInt("announcePeriod");
      modulation = Modulation.findByName(config.getString("modulation", Modulation.MSK.name()));

      if (Modulation.LoRa.equals(modulation)) {
         device = new LoRaDevice();
      } else if (Modulation.MSK.equals(modulation)) {
         device = new MSKDevice();
      } else {
         // Not a known modulation.
         throw new IOException("Unknown modulation:" + config.getString("modulation"));
      }
   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }

}
