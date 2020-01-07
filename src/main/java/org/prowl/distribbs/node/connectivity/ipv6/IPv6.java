package org.prowl.distribbs.node.connectivity.ipv6;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

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
}
