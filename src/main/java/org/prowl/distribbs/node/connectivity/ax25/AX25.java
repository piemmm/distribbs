package org.prowl.distribbs.node.connectivity.ax25;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

public class AX25 implements Connector {

   private static final Log          LOG = LogFactory.getLog("AX25");

   private HierarchicalConfiguration config;

   public AX25(HierarchicalConfiguration config) {
       this.config = config;
   }

   public void start() throws IOException {
 
   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }
}
