package org.prowl.distribbs.node.connectivity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Connectivity {

   private static final Log     LOG        = LogFactory.getLog("Connectivity");

   private SubnodeConfiguration configuration;

   private List<Connector>      connectors = new ArrayList<>();

   public Connectivity(SubnodeConfiguration configuration) {
      this.configuration = configuration;
      parseConfiguration();
   }

   /**
    * Parse the configuration and setup the connectivity nodes
    */
   public void parseConfiguration() {
      // Get a list of connectors from the config file
      List<HierarchicalConfiguration> cxs = configuration.configurationsAt("connector");

      // Go create and configure each one.
      for (HierarchicalConfiguration c : cxs) {
         String className = c.getString("type");
         try {
            Connector con = (Connector) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(c);
            connectors.add(con);
            LOG.info("Added connector: " + className);
         } catch (Throwable e) {
            // Something blew up. Log it and carry on.
            LOG.error("Unable to add connector: " + className, e);
         }

      }

      // If there are no connectors configured then exit as there's little point in continuing.
      if (connectors.size() == 0) {
         LOG.error("Not starting as no connectors have been configured");
         System.exit(1);
      }

   }

   public void start() {
      LOG.info("Starting connectivity...");
      for (Connector connector : connectors) {
         try {
            LOG.info("Starting: " + connector.getName());
            connector.start();

         } catch (Throwable e) {
            LOG.error("Unable to start connector: " + connector.getName(), e);
         }
      }
   }

}
