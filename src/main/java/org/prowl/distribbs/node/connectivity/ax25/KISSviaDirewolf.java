package org.prowl.distribbs.node.connectivity.ax25;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ka2ddo.ax25.AX25Callsign;
import org.ka2ddo.ax25.ConnState;
import org.ka2ddo.ax25.ConnectionEstablishmentListener;
import org.ka2ddo.ax25.ConnectionRequestListener;
import org.ka2ddo.ax25.io.BasicTransmittingConnector;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;
import org.prowl.distribbs.services.user.User;
import org.prowl.distribbs.uiremote.RemoteUISwitch;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Implements a KISS type passthrough on a fifo file so that things like
 * ax25-tools can play with it.
 * 
 * Data is forwarded and received on the designated rf slot (where an SX1278
 * usually resides)
 */
public class KISSviaDirewolf implements Connector {

   private static final Log          LOG        = LogFactory.getLog("KISS");

   private String                    address;
   private int                       port;
   private String                    callsign;

   private BasicTransmittingConnector connector;


   private HierarchicalConfiguration config;
   private boolean                   running;

   public KISSviaDirewolf(HierarchicalConfiguration config) {
      this.config = config;
   }

   @Override
   public void start() throws IOException {
      running = true;
      address = config.getString("address");
      port = config.getInt("port");
      callsign = config.getString("callsign");


      // Check the slot is obtainable.
      if (port < 1) {
         throw new IOException("Configuration problem - port " + port + " needs to be greater than 0");
      }


      Tools.runOnThread(()-> {
         setup();


      });









   }

   public void setup() {
      try {
         System.out.println("Connecting to kiss port");
         Socket s = new Socket(InetAddress.getByName(address), port);
         InputStream in = s.getInputStream();
         OutputStream out = s.getOutputStream();
         System.out.println("Connected to kiss port");

         // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
         // not just this one.
         AX25Callsign defaultCallsign = new AX25Callsign(callsign);


         connector = new BasicTransmittingConnector(defaultCallsign, in, out, new ConnectionRequestListener() {

            /**
             * Determine if we want to respond to this connection request (to *ANY* callsign) - usually we only accept
             * if we are interested in the callsign being sent a connection request.
             *
             * @param state      ConnState object describing the session being built
             * @param originator AX25Callsign of the originating station
             * @param port       Connector through which the request was received
             * @return
             */
            @Override
            public boolean acceptInbound(ConnState state, AX25Callsign originator, org.ka2ddo.ax25.Connector port) {

               System.out.println("Incoming connection from: " + originator.toString());

               // If we're going to accept then add a listener so we can keep track of the connection
               state.listener = new ConnectionEstablishmentListener() {
                  @Override
                  public void connectionEstablished(Object sessionIdentifier, ConnState conn) {

                     Thread tx = new Thread(() -> {

                        // Do inputty and outputty stream stuff here
                        try {



                           User user = DistriBBS.INSTANCE.getStorage().loadUser(conn.getSrc().getBaseCallsign());
                           InputStream in = state.getInputStream();
                           OutputStream out = state.getOutputStream();

                           RemoteUISwitch.newUser(user, in, out);



//                        // Get the input stream and handle incoming data in its own thread.
//                        InputStream in = state.getInputStream();
//                        Thread t = new Thread(() -> {
//                           while (state.isOpen()) {
//                              try {
//                                 System.out.println("IN:" + in.read());
//                              } catch (IOException e) {
//                                 e.printStackTrace();
//                              }
//                           }
//                           System.out.println("RX finished");
//                        });
//                        t.start();
//
//                        // Get the output stream and send something to the client (dont forget to call flush!)
//                        // will auto-'flush' when paclen is reached (or max frame size is reached)
//                        OutputStream out = state.getOutputStream();
//                        out.write("You have connected1!\r".getBytes());
//                        out.flush();
//                        try {
//                           Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                        }
//
//                        // This is how we disconnect the remote node!
//                        state.close();
                        } catch (Exception e) {
                           e.printStackTrace();
                        }

                     });

                     tx.start();


                  }

                  @Override
                  public void connectionNotEstablished(Object sessionIdentifier, Object reason) {

                  }

                  @Override
                  public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {

                  }

                  @Override
                  public void connectionLost(Object sessionIdentifier, Object reason) {

                  }
               };
               return true;
            }


         });
      } catch(Exception e) {
        LOG.error(e.getMessage(),e);
      }
   }

   @Override
   public void stop() {
      ServerBus.INSTANCE.unregister(this);
      running = false;
   }

   @Override
   public String getName() {
      return getClass().getSimpleName();
   }

   @Override
   public boolean isAnnounce() {
      return false;
   }

   @Override
   public int getAnnouncePeriod() {
      return 0;
   }

   @Override
   public Modulation getModulation() {
      return Modulation.NONE;
   }

   @Override
   public PacketEngine getPacketEngine() {
      return null;
   }

   @Override
   public boolean isRF() {
      return false;
   }

   @Override
   public boolean canSend() {
      return false;
   }

   @Override
   public boolean sendPacket(TxRFPacket packet) {
      return true;
   }

   @Override
   public int getFrequency() {
      return 0;
   }

   @Override
   public double getNoiseFloor() {
      return  0;
   }

   @Override
   public double getRSSI() {
      return 0;
   }

   public int getSlot() {
      return 0;
   }

   @Override
   public long getTxCompressedByteCount() {
      return 0;
   }

   @Override
   public long getTxUncompressedByteCount() {
      return 0;
   }
   
   @Override
   public long getRxCompressedByteCount() {
      return 0;
   }

   @Override
   public long getRxUncompressedByteCount() {
      return 0;
   }

}
