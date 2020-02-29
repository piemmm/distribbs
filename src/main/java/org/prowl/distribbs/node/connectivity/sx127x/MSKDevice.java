package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connectivity;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.EWMAFilter;
import org.prowl.distribbs.utils.Hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;

/**
 * MSK modulation based on information and examples in the semtech datasheet
 * https://www.mouser.com/datasheet/2/761/sx1276-1278113.pdf
 */
public class MSKDevice implements Device {

   private static final int      REG_FIFO             = 0x00;
   private static final int      REG_OP_MODE          = 0x01;
   private static final int      REG_LNA              = 0x0c;
   private static final int      REG_PA_CONFIG        = 0x09;

   private static final int      REG_PAYLOAD_LENGTH   = 0x32;
   private static final int      REG_DIO_MAPPING_1    = 0x40;
   private static final int      REG_VERSION          = 0x42;
   private static final int      REG_4D_PA_DAC        = 0x4d;
   private static final int      MODE_SLEEP           = 0x00;
   private static final int      MODE_STANDBY         = 0x01;
   private static final int      MODE_TX              = 0x03;
   private static final int      REG_FRF_MSB          = 0x06;
   private static final int      REG_FRF_MID          = 0x07;
   private static final int      REG_FRF_LSB          = 0x08;
   private static final int      LNA_MAX_GAIN         = 0b00100011;

   private static final int      PA_BOOST             = 0x80;

   private static final double   FREQ_STEP            = 61.03515625d;
   private static final int      SX1276_DEFAULT_FREQ  = 144950000;

   private Log                   LOG                  = LogFactory.getLog("MSKDevice");

   public SpiDevice              spi                  = Hardware.INSTANCE.getSPI();
   private GpioPinDigitalOutput  gpioSS;

   private double                rssi                 = 0;
   private double                bufferRssi           = 0;
   private int                   frequency            = SX1276_DEFAULT_FREQ;            // Our tx/rx freq
   public double                 nextHighestRSSIValue = 0;                              // quietest recorded rssi
   public double                 nextRSSIUpdate       = 0;                              // When we next update our rssi
   private long                  lastSent             = 0;                              // Time when we last finished sending a packet
   private Semaphore             spiLock              = Hardware.INSTANCE.getSPILock();
   private Semaphore             trxLock              = new Semaphore(1, true);
   private double                rssiNoiseFloor       = 0;
   private double                rssiThreshold        = 0;
   private long                  rxPacketTimeout      = 0;
   private EWMAFilter            ewmaFilter           = new EWMAFilter(0.05f);
   private long                  lastRSSISample       = 0;
   private int                   slot;

   private ByteArrayOutputStream buffer               = new ByteArrayOutputStream();

   private Connector             connector;
   private ExecutorService       pool                 = Executors.newFixedThreadPool(5);

   public MSKDevice(Connector connector, int slot, int frequency) {
      LOG = LogFactory.getLog("MSKDevice(" + slot + ")");
      this.connector = connector;
      this.slot = slot;
      this.frequency = frequency;
      init();

   }

   private void init() {
      LOG.debug("GPIO setup for MSK device");

      if (slot == 0) {
         gpioSS = Hardware.INSTANCE.getGpioSS0();
      } else if (slot == 1) {
         gpioSS = Hardware.INSTANCE.getGpioSS1();

      }

      // Get the device version
      int version = readRegister(REG_VERSION);
      if (version == 0x12) {
         // Setup a SX1276, always rx - we dont care about power usage
         LOG.info("MSK Device recognised as a SX1276");
         writeRegister(REG_OP_MODE, MODE_SLEEP);

         setChannelSX1276(frequency);

         // Rx startup regrxcfg
         // writeRegister(0x0d, 0b00011000); // set opts
         writeRegister(0x0d, 0b00011111); // set opts

         // Defaults to 19.2kbaud
         // writeRegister(0x03, 0x83); // bitrate 7:0
         // writeRegister(0x02, 0x06); // bitrate 15:8

         // Defaults to 12.5kbaud (10.5kHz deviation needed)
         // writeRegister(0x03, 0x00); // bitrate 7:0
         // writeRegister(0x02, 0x0A); // bitrate 15:8

         // Defaults to 9k6kbaud
         // writeRegister(0x03, 0x05); // bitrate 7:0
         // writeRegister(0x02, 0x0D); // bitrate 15:8

         // 1200
         // writeRegister(0x03, 0x2b); // bitrate 7:0
         // writeRegister(0x02, 0x68); // bitrate 15:8

         // 2400
         // writeRegister(0x03, 0x15); // bitrate 7:0
         // writeRegister(0x02, 0x34); // bitrate 15:8

         // 4800
         writeRegister(0x03, 0x0b); // bitrate 7:0
         writeRegister(0x02, 0x1a); // bitrate 15:8

         // regfdev (default 5khz deviation)
         // setFrequencyDeviation(10.4); // 10.4kHz
         setFrequencyDeviation(2.6); // 3kHz

         // regpaRamp
         writeRegister(0x0A, 0b00001111); // FSK
         // writeRegister(0x0A, 0b00101111); // GMFSK

         writeRegister(REG_PAYLOAD_LENGTH, 0xff);

         // Gain
         writeRegister(REG_LNA, LNA_MAX_GAIN);

         // mant exp bw 2.5
         // 10b / 24 7 2.6
         // 01b / 20 7 3.1
         // 00b / 16 7 3.9
         writeRegister(0x12, 0b00001111);
         writeRegister(0x13, 0b00010110);

         writeRegister(0x26, 32); // preamble length
         writeRegister(0x0B, 0b00111011); // reduce overcurrent protection
         writeRegister(REG_DIO_MAPPING_1, 0x10);

         // writeRegister(REG_PA_CONFIG, PA_BOOST | (17 - 2)); // 10 = power level
         // writeRegister(REG_4D_PA_DAC, 0x84);

         writeRegister(REG_PA_CONFIG, 0b11111111); // 10 = power level
         writeRegister(REG_4D_PA_DAC, 0b10000111);

         writeRegister(0x33, 0b00110101); // Node Address
         writeRegister(0x30, 0b11011000); // Setup for packet mode (not direct transmitter keying)
         writeRegister(0x31, 0b01000000); // packet mode
         writeRegister(0x35, 0b10111111); // fifo threshold (64 bytes)
         writeRegister(0x27, 0b00110110); // sync word, preamble polarity, autorestartrxmode

         // Sync word
         writeRegister(0x28, 0xff);
         writeRegister(0x29, 0x00);
         writeRegister(0x2a, 0x55);
         writeRegister(0x2b, 0xaa);
         writeRegister(0x2c, 0x00);
         writeRegister(0x2d, 0xff);

         writeRegister(0x1a, 0b00000001);
         writeRegister(0x0e, 0b00000010); // RSSI samples used

         // manual use.
         writeRegister(REG_OP_MODE, MODE_SLEEP);
         sleep(150);
         writeRegister(REG_OP_MODE, MODE_STANDBY);
         sleep(150);

         writeRegister(0x3b, 0b11000000); // auto image calibration (and do it *now*)
         sleep(150);

         writeRegister(0x24, 0b00001000);
         sleep(500);
         writeRegister(0x0d, 0b00111111); // set opts
         writeRegister(REG_OP_MODE, 0x04);
         sleep(350);
         writeRegister(REG_OP_MODE, 0x05);
         sleep(150);

      } else {

         if (version == 0) {
            // Not present
            LOG.info("Slot " + slot + " is not populated");
            throw new RuntimeException("Could not init slot " + slot);
         } else {
            LOG.error("Unknown device found! version: " + Integer.toString(version, 16));
            throw new RuntimeException("Could not init slot " + slot);
         }
      }

      Timer rxBuilder = new Timer();
      rxBuilder.schedule(new TimerTask() {

         public void run() {
            while (true) {
               try {
                  trxLock.acquireUninterruptibly();

                  // Keep in RX until full packet received.
                  int tf = readRegister(0x3f);
                  checkBuffer(false, tf);
                  rxPacketTimeout = System.currentTimeMillis() + 2000;
                  while (((buffer.size() > 0 || (readRegister(0x11) / 2d) < rssiThreshold) && System.currentTimeMillis() < rxPacketTimeout)) {
                     tf = readRegister(0x3f);
                     checkBuffer(false, tf);
                     updateRSSI();

                  }
                  updateRSSI();
               } catch (Throwable e) {
                  LOG.error(e.getMessage(), e);
               } finally {
                  trxLock.release();
               }
               try {
                  Thread.sleep(10);
               } catch (Throwable e) {
               }
            }
         }

      }, 1000);

   }

   public void setChannelSX1276(int freq) {
      freq = (int) ((double) freq / (double) FREQ_STEP);
      writeRegister(REG_FRF_MSB, (int) ((freq >> 16) & 0xFF));
      writeRegister(REG_FRF_MID, (int) ((freq >> 8) & 0xFF));
      writeRegister(REG_FRF_LSB, (int) (freq & 0xFF));
   }

   public void setFrequencyDeviation(double freq) {
      int dev = (int) ((freq * (1 << 19)) / 32000);
      writeRegister(0x04, (dev & 0xFF00) >> 8);
      writeRegister(0x05, (dev & 0x00FF));

   }

   private int readRegister(int addr) {
      int res = 0x00;
      try {
         spiLock.acquire();
         byte spibuf[] = new byte[2];
         spibuf[0] = (byte) (addr);
         spibuf[1] = 0x00;
         disableSS();
         try {
            byte[] result = spi.write(spibuf);
            res = result[1] & 0xFF;
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }
      return res;
   }

   private void writeRegister(int addr, int value) {
      try {
         spiLock.acquire();
         byte spibuf[] = new byte[2];
         spibuf[0] = (byte) (addr | 0x80); // | For SPI, 0x80 MSB set == write, clear = read.
         spibuf[1] = (byte) value;
         disableSS();
         try {
            spi.write(spibuf);
         } catch (IOException e) {
            LOG.error(e.getMessage(), e);
         }
         enableSS();
         spiLock.release();
      } catch (InterruptedException e) {
      }

   }

   private void enableSS() {
      gpioSS.high();
   }

   private void disableSS() {
      gpioSS.low();
   }

   private void getMessage(boolean crcOk) {

      try {
         // Full packet received
         if (buffer.size() > 0) {

            final byte[] array = buffer.toByteArray();
            final long rxTime = System.currentTimeMillis();
            pool.execute(new Runnable() {
               public void run() {

                  // Send to our packet engine
                  // LOG.info("MSK Rx payload(" + array.length + "): " +
                  // Tools.byteArrayToHexString(array));
                  RxRFPacket rxRfPacket = new RxRFPacket(connector, array, rxTime, bufferRssi);
                  if (!crcOk) {
                     rxRfPacket.setCorrupt();
                  }

                  // Process the packet
                  connector.getPacketEngine().receivePacket(rxRfPacket);

                  // Post the event for all and sundry
                  ServerBus.INSTANCE.post(rxRfPacket);
               }
            });
         }
      } finally {

         resetPacket();
      }
   }

   /**
    * Send a message packet
    */
   public void sendMessage(final TxRFPacket packet) {
      pool.execute(new Runnable() {
         public void run() {
            ServerBus.INSTANCE.post(packet);
            sendPacket(packet.getCompressedPacket());
         }
      });
   }

   private synchronized void sendPacket(byte[] message) {

      try {

         txDelay();

         while ((readRegister(0x11) / 2d) < rssiThreshold || buffer.size() > 0) {
            try {
               Thread.sleep(130);
            } catch (Throwable e) {
            }
         }

         trxLock.acquireUninterruptibly();
         // LOG.info("TX Start");

         // Wait for non busy channel
         while ((readRegister(0x11) / 2d) < rssiThreshold || buffer.size() > 0) {
            try {
               Thread.sleep(130);
            } catch (Throwable e) {
            }
            checkBuffer(false, readRegister(0x3f));
         }

         // Standby mode to prevent any further rx or interrupts changing fifo
         writeRegister(REG_OP_MODE, MODE_STANDBY);
         writeRegister(REG_OP_MODE, MODE_TX);
         try {
            Thread.sleep(10);
         } catch (Throwable e) {
         }
         writeRegister(REG_FIFO, (int) message.length); // fill fifo
         for (byte b : message) {
            while ((readRegister(0x3f) & 0x80) > 0) {
            }
            writeRegister(REG_FIFO, (int) b); // fill fifo
         }

         // Wait for packet to be sent
         long timeout = System.currentTimeMillis() + 1500; // 1.5 Second max for badly behaving tx
         while ((readRegister(0x3f) & 0x08) == 0 && System.currentTimeMillis() < timeout) {
            try {
               Thread.sleep(1);
            } catch (Throwable e) {
            }
         }

      } finally {
         writeRegister(REG_OP_MODE, 0x04);
         writeRegister(REG_OP_MODE, 0x05);

         lastSent = System.currentTimeMillis();
         trxLock.release();
      }

   }

   public void txDelay() {
      long delay = ((long) (Math.random() * 300d)) - (System.currentTimeMillis() - lastSent); // - Math.max(lastSent,lastReceivedOrDCD));
      if (delay > 0) {
         try {
            Thread.sleep(delay);
         } catch (Throwable e) {
         }
      }
   }

   private int     packetLength = 0;
   private boolean lengthRead   = false;

   public boolean checkBuffer(boolean completed, int x) {

      while ((x & 0x40) == 0) {
         int data = readRegister(REG_FIFO);
         rxPacketTimeout = System.currentTimeMillis() + 1000;
         if (!lengthRead) {
            packetLength = data;
            lengthRead = true;
         } else {
            if (buffer.size() == 0) {
               bufferRssi = (readRegister(0x11) / 2d);
            }
            buffer.write((byte) data);
         }

         if (buffer.size() == packetLength) {
            boolean crcOk = (x & 0x2) == 0x2;
            getMessage(true);// crcOk);
         }

         x = readRegister(0x3f);

      }

      if ((x & 0x4) != 0) {
         boolean crcOk = (x & 0x2) == 0x2;
         if (!crcOk) {
            LOG.info("Packet has bad CRC1");
         }
         getMessage(crcOk);
         return true;
      }
      // }
      return false;
   }

   public void resetPacket() {
      buffer.reset();
      lengthRead = false;
      packetLength = 9999;
   }

   public final void sleep(final long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
      }
   }

   /**
    * Sample our RSSI used for setting DCD thresholds
    */
   public void updateRSSI() {
      try {
         if (System.currentTimeMillis() > lastRSSISample) {
            lastRSSISample = System.currentTimeMillis() + 100;
            rssi = (readRegister(0x11) / 2d);
            rssiNoiseFloor = ewmaFilter.addPoint((float) rssi);
            rssiThreshold = rssiNoiseFloor - 12d;
         }
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
   }

   public double getNoiseFloor() {
      return rssiNoiseFloor;
   }

   public double getRSSI() {
      return rssi;
   }

   public int getFrequency() {
      return frequency;
   }

}