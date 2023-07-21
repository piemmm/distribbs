package org.prowl.distribbs.node.connectivity.aprs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.Digipeater;
import org.prowl.aprslib.parser.InformationField;
import org.prowl.aprslib.parser.ObjectPacket;
import org.prowl.aprslib.parser.Parser;
import org.prowl.aprslib.position.PositionPacket;
import org.prowl.aprslib.position.UnparsablePositionException;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.config.BadConfigException;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.node.connectivity.sx127x.Modulation;
import org.prowl.distribbs.node.connectivity.gps.GPS;

import com.google.common.eventbus.Subscribe;

import net.sf.marineapi.nmea.util.Position;

public class APRS extends Interface {

   private static final Log          LOG = LogFactory.getLog("APRSIS");

   private HierarchicalConfiguration config;

   /**
    * The APRS-IS password for your callsign
    */
   private String                    password;

   /**
    * The APRS-IS compatible server to connect to
    */
   private String                    server;

   /**
    * The APRS-IS port to connect to
    */
   private int                       port;

   /**
    * The announce period for the station
    */
   private long                      announcePeriod;

   /**
    * The comment to use for our APRS beacon
    */
   private String                    comment;

   /**
    * The APRS table and symbol to use (eg: "/>" is a car)
    */
   private String                    aprsTableSymbol;
   private OutputStream              out;
   /**
    * Set to true to stop all threads (used when exiting)
    */
   private boolean                   stop;

   public APRS(HierarchicalConfiguration config) {
      this.config = config;

      password = config.getString("password", "-1");
      server = config.getString("server", "rotate.aprs2.net");
      port = config.getInt("port", 14580);
      announcePeriod = config.getInt("announcePeriod", 5) * 1000l * 60l; // Announce Period in minutes for sending our position
      comment = config.getString("comment", DistriBBS.VERSION_STRING + " build " + DistriBBS.BUILD);
      aprsTableSymbol = config.getString("aprsSymbols", "/#");

   }

   public void validateConfig() throws IOException {
      // Validation
      if (aprsTableSymbol.length() > 2) {
         throw new BadConfigException("Invalid APRS symbol (should be 2 characters)");
      }

   }

   @Override
   public void start() throws IOException {
      validateConfig();
      Thread t = new Thread() {
         public void run() {
            startRFSend();
            while (!stop) {
               try {
                  Thread.sleep(20000);
                  connect();
               } catch (Throwable e) {
                  LOG.error(e.getMessage(), e);
               }
            }
         }
      };
      t.start();
      ServerBus.INSTANCE.register(this);
   }

   @Override
   public void stop() {
      stop = true;
      ServerBus.INSTANCE.unregister(this);
   }

   /**
    * Connect to APRS-IS server
    */
   public void connect() {
      Position position = GPS.getCurrentPosition();
      if (position == null) {
         LOG.info("Waiting for GPS before connecting to APRS-IS");
         return; // No position so we don't bother connecting at the moment as we don't know our
                 // position
      }

      try {

         NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(2);
         nf.setMinimumFractionDigits(2);

         Socket s = new Socket(InetAddress.getByName(server), port);
         try {
            s.setSoTimeout(300000);
         } catch (Throwable e) {
         }
         try {
            s.setKeepAlive(true);
         } catch (Throwable e) {
         }
         InputStream in = s.getInputStream();
         out = s.getOutputStream();

         String lat = nf.format(position.getLatitude());
         String lon = nf.format(position.getLongitude());
         String filter = "filter r/" + lat + "/" + lon + "/25";

         // Login to APRS-IS
         out.write(("user " + DistriBBS.INSTANCE.getMyCall() + " pass " + password + " vers " + DistriBBS.NAME + " " + DistriBBS.VERSION + " " + filter + "\n").getBytes());
         out.flush();

         LOG.info("Connected to APRS-IS");

         /**
          * This controls the reporting of *our* station to APRS-IS
          */
         Timer sendPositionTimer = new Timer();
         sendPositionTimer.schedule(new TimerTask() {
            public void run() {

               try {
                  Position position = GPS.getCurrentPosition();
                  if (position != null) {
                     org.prowl.aprslib.position.Position aprsPosition = new org.prowl.aprslib.position.Position(position.getLatitude(), position.getLongitude());
                     aprsPosition.setSymbolTable('/');
                     aprsPosition.setSymbolCode('>');
                     PositionPacket op = new PositionPacket(aprsPosition, comment, false);
                     List<Digipeater> digis = new ArrayList();
                     digis.add(Digipeater.get("APDBBS"));
                     digis.add(Digipeater.get("TCPIP*"));

                     APRSPacket packet = new APRSPacket(DistriBBS.INSTANCE.getMyCall(), "", digis, op);
                     LOG.info("Sending APRS-IS packet: " + packet.toString());
                     aprsisWrite((packet.toString() + "\n").getBytes());
                  }
               } catch (Throwable e) {
                  LOG.error(e.getMessage(), e);
               }
            }
         }, 5000, Math.max(60000l, announcePeriod));

         BufferedReader reader = new BufferedReader(new InputStreamReader(in), 32768);

         while (!s.isClosed()) {

            String line = reader.readLine();
            if (!line.startsWith("#")) {
               parsePacket(line, System.currentTimeMillis());
            }
         }
         out = null;
         sendPositionTimer.cancel();

      } catch (Throwable e) {
         e.printStackTrace();
      }
   }

   public void aprsisWrite(byte[] data) throws IOException {
      out.write(data);
      out.flush();
   }

   /**
    * Gate packets heard from RF to APRS-IS
    * 
    * @param packet
    */
   @Subscribe
   public void processPacket(RxRFPacket packet) {
      // Check for APRS packets and process accordingly
      String destination = packet.getDestination();
      if (destination.startsWith(PacketTools.APRS + ",")) {
         try {
            String aprsString = packet.getSource() + ">" + packet.getDestination() + ":" + new String(packet.getPayload());
            LOG.info("RX APRS Packet:" + aprsString);

            APRSPacket aprsPacket = Parser.parse(aprsString);

            List<Digipeater> digipeaters = aprsPacket.getDigipeaters();

            // If we've already processed this, then stop.
            if (digiContains(digipeaters, DistriBBS.INSTANCE.getMyCall())) {
               return;
            }

            // TODO: Process it locally, add to a map, quadtree, etc?

            // Add our callsign to the list of digis if not already seen
            digipeaters.add(Digipeater.get(DistriBBS.INSTANCE.getMyCall()));

            // Should we rebroadcast if not connected to APRS-IS?

            // Lastly, send to APRS-IS if connected
            if (out != null) {
               aprsisWrite((aprsPacket.getOriginalString() + "\n").getBytes());
            }

         } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
         }
      }

   }

   public void parsePacket(String packet, long time) {
      try {
         APRSPacket p = Parser.parse(packet, time);
         InformationField f = p.getAprsInformation();
         if (f instanceof ObjectPacket) {
            parseObjectPacket(p, (ObjectPacket) f);
         } else if (f instanceof PositionPacket && ((PositionPacket) f).getPosition() != null) {
            parsePositionPacket(p, (PositionPacket) f);
         }

         // DEBUG: Gate packets temporarily so we can have some traffic
         // p.getDigipeaters().add(Digipeater.get(DistriBBS.INSTANCE.getMyCall());
         // String ds = p.getDigiString().substring(0,p.getDigiString().length()-1);
         // TxRFPacket txPacket = new TxRFPacket(DistriBBS.INSTANCE.getMyCall(), ds, "",
         // p.getAprsInformation().toString().getBytes());
         // DistriBBS.INSTANCE.getConnectivity().getPort(0).sendPacket(txPacket);

      } catch (UnparsablePositionException e) {
         LOG.error(e.getMessage(), e);
         // Don't care
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
   }

   /**
    * Check a list of digipeaters
    * 
    * @param digis
    * @return
    */
   public boolean digiContains(List<Digipeater> digis, String searchCall) {
      for (Digipeater d : digis) {
         if (d.getCallsign().equalsIgnoreCase(searchCall)) {
            return true;
         }
      }
      return false;
   }

   public void parsePositionPacket(APRSPacket p, PositionPacket f) {
      // APRSLookup.INSTANCE.add(p, f.getPosition());
   }

   public void parseObjectPacket(APRSPacket p, ObjectPacket f) {
      // APRSLookup.INSTANCE.add(p, f.getPosition());
   }

   /**
    * Periodically broadcasts an APRS packet out for this station over RF
    */
   public void startRFSend() {
      Timer rfSendPositionTimer = new Timer();
      rfSendPositionTimer.schedule(new TimerTask() {

         private Position lastGPSPosition;
         private long     lastSendTime;
         private Double   lastHeading = Double.valueOf(0);

         public void run() {
            try {
               Position position = GPS.getCurrentPosition();
               Double currentHeading = GPS.getCurrentCourse();
               if (position == null) {
                  return; // No GPS so do nothing.
               }
               boolean shouldSend = false;
               if (announcePeriod == 0) {
                  // Automatic position reporting delay (10minute maximum)
                  double distance = 0;
                  if (lastGPSPosition != null) {
                     distance = position.distanceTo(lastGPSPosition);
                  }
                  double bearingChange = (lastHeading - currentHeading + 360) % 360;
                  // If we are moving
                  if (distance > 200) {
                     // And the angle we are travelling it changes too much, or 2 minutes pass
                     if (bearingChange > 30 || System.currentTimeMillis() - lastSendTime > 120000) {
                        shouldSend = true;
                     }
                  } else {
                     // If more than 5 minutes stationary then send
                     if (System.currentTimeMillis() - lastSendTime > 300000l) {
                        shouldSend = true;
                     }
                  }

               } else {
                  // Fixed delay between position reports (5 minute minimum)
                  if (System.currentTimeMillis() - lastSendTime > announcePeriod) {
                     shouldSend = true;
                  }
               }
               if (shouldSend) {
                  try {
                     if (position != null) {
                        org.prowl.aprslib.position.Position aprsPosition = new org.prowl.aprslib.position.Position(position.getLatitude(), position.getLongitude());
                        aprsPosition.setSymbolTable(aprsTableSymbol.charAt(0));
                        aprsPosition.setSymbolCode(aprsTableSymbol.charAt(1));
                        PositionPacket op = new PositionPacket(aprsPosition, comment, false);
                        List<Digipeater> digis = new ArrayList();
                        digis.add(Digipeater.get("APRS"));
                        digis.add(Digipeater.get("WIDE1-1"));

                        APRSPacket packet = new APRSPacket(DistriBBS.INSTANCE.getMyCall(), "", digis, op);
                        TxRFPacket txPacket = new TxRFPacket(DistriBBS.INSTANCE.getMyCall(), packet.getDigiString(), "", packet.getAprsInformation().toString().getBytes());
                        DistriBBS.INSTANCE.getInterfaceHandler().getPort(0).sendPacket(txPacket);
                        lastGPSPosition = position;
                        lastSendTime = System.currentTimeMillis();
                        lastHeading = currentHeading;
                     }
                  } catch (Throwable e) {
                     LOG.error(e.getMessage(), e);
                  }
               }
            } catch (Throwable e) {
               LOG.error(e.getMessage(), e);
            }
         }
      }, 5000, 3000);
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
      return null;
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
      return false;
   }

   @Override
   public int getFrequency() {
      return 0;
   }

   @Override
   public double getNoiseFloor() {
      return 0;
   }

   @Override
   public double getRSSI() {
      return 0;
   }

   @Override
   public int getSlot() {
      return -1;
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
