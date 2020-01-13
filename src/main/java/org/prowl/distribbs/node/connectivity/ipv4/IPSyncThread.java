package org.prowl.distribbs.node.connectivity.ipv4;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The sync thread is responsible for negotiating a sync of messages between 2
 * clients, as well as propagating thigns like aprs and chat messages througout
 * the node network
 */
public class IPSyncThread extends Thread {

   private static final Log LOG = LogFactory.getLog("IPSyncThread");

   private Socket           socket;
   private IPv4             ipv4;

   public IPSyncThread(Socket socket, IPv4 ipv4) {
      this.socket = socket;
      this.ipv4 = ipv4;
   }

   public void run() {

      try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

         // Socket options
         socket.setKeepAlive(true); // Keep active connection
         socket.setTcpNoDelay(false); // Small delays acceptable
         socket.setSoTimeout(30000); // 30 second timeout.

         // First, wait for a simple authentication string (or timeout).
         String auth = in.readUTF(); // Allowed to read timeout, if it does it won't match the secret and the socket will close.
         if (auth.contentEquals(ipv4.getPeerSecret())) {
            socket.setSoTimeout(300000); // Now set timeouts to 5 minutes
            beginSync(in, out);
         } 
         
         // Close the socket - we're done.
         socket.close();
      } catch (IOException e) {
         LOG.warn(e);
      }

   }

   /**
    * The client has authenticated enough for us to trust it. So we begin a sync
    * @param in
    * @param out
    */
   private void beginSync(DataInputStream in, DataOutputStream out) {

      
      
      
   }

}
