package org.prowl.distribbs.node.connectivity.sx127x;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AnnounceTest extends Thread {

   private static final Log LOG = LogFactory.getLog("AnnounceTest");

   public AnnounceTest() {

   }

   public void run() {
      MSKDevice ld = new MSKDevice();

      while (true) {

         LOG.info("Sending test message");
         ld.sendMessage("STARThiiihuihbiohboihbioubhiouboiuboiboiuboiuboiubiouboiuboiuboiuboiuboiubiuyvEND".getBytes());

         try {
            Thread.sleep(1100);
         } catch (Throwable e) {
         }
      }
   }

   public static void main(String[] args) {

      AnnounceTest la = new AnnounceTest();
      la.start();

   }

}