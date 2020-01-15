package org.prowl.distribbs;

import org.prowl.distribbs.config.Config;
import org.prowl.distribbs.node.connectivity.Connectivity;
import org.prowl.distribbs.objectstorage.Storage;

/**
 * DistriBBS starting class
 * 
 * Loads the configuration and starts the node
 */
public enum DistriBBS {

   INSTANCE;

   private Config       configuration;
   private Connectivity connectivity;
   private Storage      storage;

   DistriBBS() {
   }

   public void startup() {

      // Load configuration and initialise everything needed.
      configuration = new Config();

      // Init object storage
      storage = new Storage(configuration.getConfig("storage"));
      
      // Init node connectors
      connectivity = new Connectivity(configuration.getConfig("connectivity"));

      // Init node services
      connectivity.start();

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

   
   
   public Storage getStorage() {
      return storage;
   }

   public static void main(String[] args) {
      INSTANCE.startup();
   }
}
