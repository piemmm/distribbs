package org.prowl.distribbs.node.connectivity.ax25;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.node.connectivity.sx127x.Modulation;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements a KISS type passthrough on a fifo file so that things like
 * ax25-tools can play with it.
 * 
 * Data is forwarded and received on the designated rf slot (where an SX1278
 * usually resides) and is then forwarded to a fifo socket made by this app which
 * then allows ax25-tools in linux to connect to it.
 */
public class KISSviaSX127x extends Interface {

   private static final Log          LOG        = LogFactory.getLog("KISSviaSX127x");

   private static final int          KISS_FEND  = 0xc0;
   private static final int          KISS_FESC  = 0xDB;
   private static final int          KISS_TFEND = 0xDC;
   private static final int          KISS_TFESC = 0xDD;

   private String                    interfaceName;
   private int                       bindToSlot;

   private Thread                    pipeInput;
   private Thread                    pipeOutput;
   private Object                    MONITOR    = new Object();
   private LinkedList<byte[]>        outBuffer  = new LinkedList<>();

   private HierarchicalConfiguration config;
   private boolean                   running;

   private long                      txCompressedByteCount;
   private long                      rxCompressedByteCount;
   private long                      txUncompressedByteCount;
   private long                      rxUncompressedByteCount;

   public KISSviaSX127x(HierarchicalConfiguration config) {
      this.config = config;
   }

   @Override
   public void start() throws IOException {
      running = true;
      interfaceName = config.getString("interfaceName");
      bindToSlot = config.getInt("bindToSlot");

      // Check the slot is obtainable.
      if (getSlot(bindToSlot) == null) {
         throw new IOException("Configuration problem - radio for slot " + bindToSlot + " for configured ax25 interface");
      }

      String input = "lax0";
      String output = "lax0";

      createFifoPipe(input);
      createFifoPipe(output);

      // KISS input thread
      pipeInput = new Thread() {

         private boolean               inEscape;
         private boolean               inCommand;
         private ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

         public void run() {
            int command = 0;
            int portOrChecksum = 0;

            try {
               BufferedInputStream in = new BufferedInputStream(new FileInputStream(input), 9000);

               while (running) {
                  int b = in.read();
                  if (b != -1) {

                     if (b == KISS_FEND) {
                        if (bout.size() > 0) {
                           processInKiss(bout.toByteArray(), command, portOrChecksum);
                        }
                        bout.reset();
                        inCommand = true;
                        inEscape = false;
                     } else if (inCommand) {
                        command = b & 0x0f;
                        portOrChecksum = (b >> 4) & 0x0f;
                        inCommand = false;
                        inEscape = false;
                     } else if (inEscape) {
                        if (b == KISS_TFESC) {
                           bout.write(KISS_FESC);
                        } else if (b == KISS_TFEND) {
                           bout.write(KISS_FEND);
                        }
                        inCommand = false;
                        inEscape = false;
                     } else {

                        if (b == KISS_FESC) {
                           inEscape = true;
                           inCommand = false;
                        } else {
                           bout.write(b);
                        }

                     }
                  }
               }

            } catch (IOException e) {
               LOG.error(e.getMessage(), e);
            }
         }

         public void processInKiss(byte[] kissData, int command, int checksum) {
            // LOG.info("KISSRX: command:" + command + " checksum:" + checksum + " data:" +
            // Tools.byteArrayToHexString(kissData));

            if (command == 0) {
               TxRFPacket packet = new TxRFPacket("", "", PacketTools.KISS, kissData);
               sendPacket(packet);
            } else {
               LOG.info("Ignored kiss command: " + command + " (port/checksum: " + checksum);
            }
         }
      };

      // Output thread (de-encapsulate incoming data relevant to us, and then pipe
      // out)
      pipeOutput = new Thread() {

         public void run() {
            ServerBus.INSTANCE.register(this);

            try {
               BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output), 2000);
               while (running) {
                  try {
                     synchronized (MONITOR) {
                        MONITOR.wait(1000);
                     }
                  } catch (InterruptedException e) {
                  }
                  while (outBuffer.size() > 0) {
                     try {
                        byte[] ba = outBuffer.removeFirst();
                        // LOG.info("KISS Tx:" + ba.length + " data:" + Tools.byteArrayToHexString(ba));

                        out.write(ba);
                        out.flush();
                     } catch (Throwable e) {
                        LOG.debug(e.getMessage(), e);
                     }
                  }

               }

            } catch (IOException e) {
               LOG.error(e.getMessage(), e);
            }
         }

         @Subscribe
         public void rxPacket(RxRFPacket packet) {

            try {
               // Ignore corrupt packets in the heard list.
               // if (packet.isCorrupt()) {
               // return;
               // }

               if (DistriBBS.INSTANCE.getMyCall().equals(packet.getSource())) {
                  return;
               }

               if (PacketTools.KISS.equals(packet.getCommand())) {
                  // LOG.info("KISS RF Rx:" + packet.getSource() + ">" + packet.getDestination() +
                  // ": Size:" + packet.getPayload().length + " " +
                  // Tools.byteArrayToHexString(packet.getPayload()));

                  // Escape stuff.
                  ByteArrayOutputStream bos = new ByteArrayOutputStream();
                  bos.write(KISS_FEND);
                  bos.write(0); // data command
                  byte[] data = packet.getPayload();
                  for (int i : data) {
                     int j = (i & 0xFF);
                     if (j == KISS_FEND) {
                        bos.write(KISS_FESC);
                        bos.write(KISS_TFEND);
                     } else if (j == KISS_FESC) {
                        bos.write(KISS_FESC);
                        bos.write(KISS_TFESC);
                     } else {
                        bos.write(j);
                     }

                  }
                  bos.write(KISS_FEND);

                  outBuffer.add(bos.toByteArray());
                  bos.close();
                  synchronized (MONITOR) {
                     MONITOR.notifyAll();
                  }
               }

            } catch (Throwable e) {
               LOG.error(e.getMessage(), e);
            }
         }
      };

      pipeOutput.start();
      pipeInput.start();
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
      return getSlot(bindToSlot).getModulation();
   }

   @Override
   public PacketEngine getPacketEngine() {
      return null;
   }

   @Override
   public boolean isRF() {
      return true;
   }

   @Override
   public boolean canSend() {
      return true;
   }

   @Override
   public boolean sendPacket(TxRFPacket packet) {
      return getSlot(bindToSlot).sendPacket(packet);
   }

   @Override
   public int getFrequency() {
      return getSlot(bindToSlot).getFrequency();
   }

   @Override
   public double getNoiseFloor() {
      return getSlot(bindToSlot).getNoiseFloor();
   }

   @Override
   public double getRSSI() {
      return getSlot(bindToSlot).getRSSI();
   }

   public int getSlot() {
      return bindToSlot;
   }

   public Interface getSlot(int slot) {
      List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getPorts();
      for (Interface connector : connectors) {
         if (!(connector instanceof KISSviaSX127x)) {
            if (connector.getSlot() == slot) {
               return connector;
            }

         }
      }
      return null;
   }

   public File createFifoPipe(String name) throws IOException {
      try {
         // new File(name).delete();
         Process process = null;
         String[] command = new String[] { "mkfifo", name };
         process = new ProcessBuilder(command).inheritIO().start();
         process.waitFor();
      } catch (InterruptedException e) {
         throw new IOException(e);
      }
      return new File(name);
   }

   @Override
   public long getTxCompressedByteCount() {
      return getSlot(bindToSlot).getTxCompressedByteCount();
   }

   @Override
   public long getTxUncompressedByteCount() {
      return getSlot(bindToSlot).getTxUncompressedByteCount();
   }
   
   @Override
   public long getRxCompressedByteCount() {
      return getSlot(bindToSlot).getRxCompressedByteCount();
   }

   @Override
   public long getRxUncompressedByteCount() {
      return getSlot(bindToSlot).getRxUncompressedByteCount();
   }

}
