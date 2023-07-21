package org.prowl.distribbs;

import java.io.IOException;
import java.util.Locale;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.config.Config;
import org.prowl.distribbs.node.connectivity.InterfaceHandler;
import org.prowl.distribbs.objects.Storage;
import org.prowl.distribbs.statistics.Statistics;
import org.prowl.distribbs.services.ServiceHandler;
import org.prowl.distribbs.ui.hardware.Status;


/**
 * DistriBBS starting class
 * 
 * Loads the configuration and starts the node
 */
public enum DistriBBS {

   INSTANCE;

   static {
      // Set our default log format before any logger is created
      System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
   }

   public static final String NAME           = "DistriBBS";
   public static final String VERSION        = "0.02";
   public static final long   BUILD          = 2023071701;
   public static final String VERSION_STRING = NAME + " v" + VERSION;
   public static final String INFO_TEXT      = "   www.distribbs.net";
   private static final Log   LOG            = LogFactory.getLog("DistriBBS");

   private Config             configuration;
   private InterfaceHandler interfaceHandler;
   private Storage            storage;
   private Status             status;
   private Statistics         statistics;
   private ServiceHandler serviceHandler;
   private String             myCall;
   private String             myBBSAddress;

   DistriBBS() {
   }

   public void startup() {

      try {
         // Init resource bundles.
         Messages.init();

         // Load configuration and initialise everything needed.
         configuration = new Config();
   
         // Set our callsign
         myCall = DistriBBS.INSTANCE.getConfiguration().getConfig("callsign", "NOCALL").toUpperCase(Locale.ENGLISH);

         // Set out BBS address which helps with message routing
         myBBSAddress = DistriBBS.INSTANCE.getConfiguration().getConfig("bbsAddress", "NOCALL.#00.XXX.XXX").toUpperCase(Locale.ENGLISH);

         // Create statistics holder
         statistics = new Statistics();
   
         // Init status objects
         status = new Status();
   
         // Init object storage
         storage = new Storage(configuration.getConfig("storage"));

         // Init services
         serviceHandler = new ServiceHandler(configuration.getConfig("services"));

         // Init interfaces
         interfaceHandler = new InterfaceHandler(configuration.getConfig("interfaces"));

         // Start node services
         serviceHandler.start();

         // Start interfaces
         interfaceHandler.start();
   
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
         
      } catch(IOException e) {
         LOG.error(e.getMessage(),e);
         System.exit(1);
      }
   }

   public String getBBSServices() {
      return "X";
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

   public InterfaceHandler getInterfaceHandler() {
      return interfaceHandler;
   }

   public ServiceHandler getServiceHandler() {  return serviceHandler;  }

   public static void main(String[] args) {
      INSTANCE.startup();
   }

   public String getMyCall() {
      return myCall;
   }

   public String getMyCallNoSSID() {
      if (!myCall.contains("-")) {
         return myCall;
      }
      return myCall.substring(0, myCall.indexOf('-'));
   }

   /**
    * Address used to route messages if linking the BBS to the greater network
    * @return
    */
   public String getBBSAddress() {
      return myBBSAddress;
   }

}
