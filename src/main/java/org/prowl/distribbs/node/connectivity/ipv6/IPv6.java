package org.prowl.distribbs.node.connectivity.ipv6;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;

public class IPv6 implements Connector {

   private static final Log          LOG = LogFactory.getLog("IPv6");

   private HierarchicalConfiguration config;
   private String sharedSecret;

   public IPv6(HierarchicalConfiguration config) {
       this.config = config;
   }

   public void start() throws IOException {
      sharedSecret = config.getString("sharedSecret");

   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }
   
   public boolean isAnnounce() {
      return false;
   }

   public int getAnnouncePeriod() {
      return 0;
   }

   public Modulation getModulation() {
      return Modulation.NONE;
   }
   
   public boolean isRF() {
      return false;
   }
   
   public boolean canSend() {
      return true;
   }

   public boolean sendPacket(byte[] data) {
      return false;
   }
}
