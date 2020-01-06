package org.prowl.distribbs.node.connectivity.ipv6;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

public class IPv6 implements Connector {

   private static final Log LOG = LogFactory.getLog("IPv6");

  
   public IPv6() {
      
   }
   
   public void start() {

   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }
}
