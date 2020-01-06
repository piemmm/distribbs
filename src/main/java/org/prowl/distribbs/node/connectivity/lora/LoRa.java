package org.prowl.distribbs.node.connectivity.lora;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

/**
 * Implements a LoRa based communications node
 */
public class LoRa implements Connector {

   private static final Log LOG = LogFactory.getLog("LoRa");
   
   public LoRa() {
      LOG.info("LoRa connector starting up");
      
      
   }
   
   public void start() {
      
   }
   
   public void stop() {
      
   }

   public String getName() {
      return getClass().getName();
   }

}
