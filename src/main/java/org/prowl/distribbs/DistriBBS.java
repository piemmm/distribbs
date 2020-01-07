package org.prowl.distribbs;

import org.prowl.distribbs.config.Config;
import org.prowl.distribbs.node.connectivity.Connectivity;

/**
 * DistriBBS starting class
 * 
 * Loads the configuration and starts the node
 */
public enum DistriBBS {

   INSTANCE;

   private Config       configuration;
   private Connectivity connectivity;

   DistriBBS() {
   }

   public void startup() {

      // Load configuration and initialise everything needed.
      configuration = new Config();

      // Init object storage


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

               } catch(InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
      };
      t.start();
   }

   public static void main(String[] args) {
      INSTANCE.startup();
   }
}
