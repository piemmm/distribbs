package org.prowl.distribbs.services.messages.node.connectivity;

import java.net.Inet4Address;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.distribbs.node.connectivity.ipv4.IPv4;

import junit.framework.TestCase;

public class IPv4Test extends TestCase {

   
   public void testCrypt() {
      HierarchicalConfiguration config = new HierarchicalConfiguration();
      config.setProperty("peerSecret","This is a secret test");
      config.setProperty("remoteCallsign", "G0SGY");
      config.setProperty("listenIp","127.0.0.1");
      config.setProperty("listenPort", 2345);
      config.setProperty("remoteIp","127.0.0.1");
      config.setProperty("remotePort", 2345);
      IPv4 ipv4 = new IPv4(config);
      
   }
   
}
