package org.prowl.distribbs.node.connectivity.ipv4;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

public class IPv4 implements Connector {

   private static final Log          LOG = LogFactory.getLog("IPv4");

   private HierarchicalConfiguration config;
   private String sharedSecret;

   public IPv4(HierarchicalConfiguration config) {
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

}
