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
         ld.sendMessage("STARThiiiuvuviugviuytczcoi0a9w8hindoq2 dcos;hvl8eifnaldwh9c;svoiweclknco;a8h3iwniyutciyciyciyuiuyviuyviuvuivguiviuviuviuvuyiviuyvguvjkhgviuygfuyvEND".getBytes());

         try {
            Thread.sleep(1500);
         } catch (Throwable e) {
         }
      }
   }

   public static void main(String[] args) {

      AnnounceTest la = new AnnounceTest();
      la.start();

   }

}