package org.prowl.distribbs.node.connectivity.lora;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

/**
 * Implements a LoRa based communications node
 */
public class LoRa implements Connector {

   private static final Log          LOG = LogFactory.getLog("LoRa");

   private HierarchicalConfiguration config;
   
   /**
    * Should we announce?
    */
   private boolean announce;
   
   /**
    * Announce period in minutes if no tx activity from us(absolute minimum 15 minutes)
    */
   private int announcePeriod; 

   public LoRa(HierarchicalConfiguration config) {
       this.config = config;

   }

   public void start() throws IOException {
      announce = config.getBoolean("announce");
      announcePeriod = config.getInt("announcePeriod");
      
      
   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }

}
