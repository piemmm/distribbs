package org.prowl.distribbs.node.connectivity.ax25;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

public class AX25 implements Connector {

   private static final Log LOG = LogFactory.getLog("AX25");

   public AX25() {
      
   }
   
   public void start() {

   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }
}
