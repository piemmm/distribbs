package org.prowl.distribbs;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.config.Config;
import org.prowl.distribbs.node.connectivity.Connectivity;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.statistics.Statistics;
import org.prowl.distribbs.ui.UI;
import org.prowl.distribbs.ui.hardware.Status;

/**
 * DistriBBS starting class
 * 
 * Loads the configuration and starts the node
 */
public enum DistriBBS {

   INSTANCE;

   public static final String VERSION        = "0.02";
   public static final long   BUILD          = 2020021401;
   public static final String VERSION_STRING = "DistriBBS v" + VERSION;
   public static final String INFO_TEXT      = "   www.distribbs.net";
   private static final Log   LOG            = LogFactory.getLog("DistriBBS");

   private Config             configuration;
   private Connectivity       connectivity;
   private Storage            storage;
   private Status             status;
   private Statistics         statistics;
   private UI                 ui;
   private String myCall;

   DistriBBS() {
   }

   public void startup() {

      // Load configuration and initialise everything needed.
      configuration = new Config();
      
      // Set our callsign
      myCall = DistriBBS.INSTANCE.getConfiguration().getConfig("callsign", "NOCALL").toUpperCase(Locale.ENGLISH);
      
      // Create statistics holder
      statistics = new Statistics();   
      
      // Init status objects
      status = new Status();

      // Init object storage
      storage = new Storage(configuration.getConfig("storage"));

      // Init connectors
      connectivity = new Connectivity(configuration.getConfig("connectivity"));

      // Start connectors
      connectivity.start();

      // Init User interfaces
      ui = new UI(configuration.getConfig("ui"));

      // Start node services
      ui.start();

      // All done
      Thread t = new Thread() {
         public void run() {
            while (true) {
               try {
                  Thread.sleep(1000);

               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
      };
      t.start();
   }

   public Config getConfiguration() {
      return configuration;
   }

   public Status getStatus() {
      return status;
   }

   public Storage getStorage() {
      return storage;
   }
   
   public Statistics getStatistics() {
      return statistics;
   }
   
   public Connectivity getConnectivity() {
      return connectivity;
   }

   public static void main(String[] args) {
      INSTANCE.startup();
   }
   
   public String getMyCall() {
      return myCall;
   }
   
  
}
