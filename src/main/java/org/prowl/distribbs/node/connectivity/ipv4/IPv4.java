package org.prowl.distribbs.node.connectivity.ipv4;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.NewAPRSMessage;
import org.prowl.distribbs.eventbus.events.NewChatMessage;
import org.prowl.distribbs.eventbus.events.NewMailMessage;
import org.prowl.distribbs.eventbus.events.NewNewsMessage;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

import com.google.common.eventbus.Subscribe;

public class IPv4 implements Connector {

   private static final Log          LOG          = LogFactory.getLog("IPv4");

   private static final int          DEFAULT_PORT = 1180;

   private HierarchicalConfiguration config;
   private String                    peerSecret;
   private String                    remoteCallsign;
   private InetAddress               listenIp;
   private int                       listenPort;
   private InetAddress               remoteIp;
   private int                       remotePort;

   private ListeningThread           listeningThread;
   private ConnectingThread          connectingThread;

   public IPv4(HierarchicalConfiguration config) {
      this.config = config;
   }

   public void start() throws IOException {
      try {
         peerSecret = config.getString("peerSecret");
         remoteCallsign = config.getString("remoteCallsign");
         listenIp = Inet4Address.getByName(config.getString("listenIp"));
         listenPort = config.getInt("listenPort", DEFAULT_PORT);
         remoteIp = Inet4Address.getByName(config.getString("remoteIp"));
         remotePort = config.getInt("remotePort", DEFAULT_PORT);

         // Start the thread for incoming connections
         listeningThread = new ListeningThread();
         listeningThread.start();

         // Start the thread that connects us to the remote
         // Only the connecting, or the listening thread have a
         // connection. The newest connection will be kept whilst the
         // oldest dropped.
         connectingThread = new ConnectingThread();
         connectingThread.start();

         // Start listening to events.
         ServerBus.INSTANCE.register(this);
      } catch (Throwable e) {
         throw new IOException(e);
      }
   }

   public void stop() {
      ServerBus.INSTANCE.unregister(this);
   }

   @Subscribe
   public void newNewsEvent(NewNewsMessage e) {

   }

   @Subscribe
   public void newMailEvent(NewMailMessage e) {

   }

   @Subscribe
   public void newChatEvent(NewChatMessage e) {

   }

   @Subscribe
   public void newAPRSEvent(NewAPRSMessage e) {

   }

   public String getName() {
      return getClass().getName();
   }

   private class ListeningThread extends Thread {

      private boolean      stop;
      private ServerSocket incoming;

      public ListeningThread() {

      }

      public void run() {
         while (!stop) {
            try {
               // Socket creation in a loop because a listening socket can be
               // closed when a port flaps.
               incoming = new ServerSocket(listenPort, 4, listenIp);

               try {
                  while (!stop) {
                     Socket connectedClient = incoming.accept();
                     IPSyncThread sync = new IPSyncThread(connectedClient, IPv4.this);
                     sync.start();
                  }
               } catch(Throwable e) {
                  // incoming probably got canned.
                  LOG.error(e);
               }
               
            } catch (IOException e) {
               LOG.error(e);
            }
            Tools.sleep(1000);
         }
      }

      public void stopNow() {

      }
   }

   private class ConnectingThread extends Thread {

      private boolean stop;

      public ConnectingThread() {

      }

      public void run() {
         while (!stop) {
            
         }
      }

      public void stopNow() {

      }

   }

   String getPeerSecret() {
      return peerSecret;
   }

   
   
}
