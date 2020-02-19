package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;

/**
 * MSK modulation based on information and examples in the semtech datasheet
 * https://www.mouser.com/datasheet/2/761/sx1276-1278113.pdf
 */
public class MSKDevice implements Device {

   private static final int      REG_FIFO            = 0x00;
   private static final int      REG_OP_MODE         = 0x01;
   private static final int      REG_OCP             = 0x0b;
   private static final int      REG_LNA             = 0x0c;
   // private static final int REG_FIFO_ADDR_PTR = 0x0D;
   private static final int      REG_PA_CONFIG       = 0x09;
   // private static final int REG_FIFO_RX_CURRENT_ADDR = 0x10;
   private static final int      REG_IRQ_FLAGS       = 0x3e;
   private static final int      REG_IRQ_FLAGS2      = 0x3f;
//   private static final int     REG_RX_NB_BYTES          = 0x13;

   private static final int      REG_PAYLOAD_LENGTH  = 0x32;
   // private static final int REG_MAX_PAYLOAD_LENGTH = 0x23;
   private static final int      REG_SYNC_WORD       = 0x39;
   private static final int      REG_DIO_MAPPING_1   = 0x40;
   private static final int      REG_DIO_MAPPING_2   = 0x41;
   private static final int      REG_VERSION         = 0x42;
   private static final int      REG_4D_PA_DAC       = 0x4d;
   private static final int      MODE_SLEEP          = 0x00;
   private static final int      MODE_STANDBY        = 0x01;
   private static final int      MODE_TX             = 0x03;
   private static final int      MODE_RX_CONTINUOUS  = 0x06;
   private static final int      REG_FRF_MSB         = 0x06;
   private static final int      REG_FRF_MID         = 0x07;
   private static final int      REG_FRF_LSB         = 0x08;
   private static final int      LNA_MAX_GAIN        = 0b00100011;
   private static final int      LNA_MIN_GAIN        = 0b10100010;

   private static final int      IRQ_TX_DONE_MASK    = 0x8;
   private static final int      PA_BOOST            = 0x80;

   private static final double   FREQ_STEP           = 61.03515625d;
//   private static final int      SX1276_DEFAULT_FREQ = 868100000;
//   private static final int      SX1276_DEFAULT_FREQ = 434100000;
   private static final int      SX1276_DEFAULT_FREQ = 144950000;

   private static final Log      LOG                 = LogFactory.getLog("MSKDevice");

   public static SpiDevice       spi                 = null;
   private GpioController        gpio;
   private GpioPinDigitalOutput  gpioSS;
   private GpioPinDigitalInput   gpioDio;
   private Pin                   dio                 = RaspiPin.GPIO_07;
   private Pin                   ss                  = RaspiPin.GPIO_06;

   private boolean               tx                  = false;                         // True when in TX.
   private Semaphore             spiLock             = new Semaphore(1);

   private ByteArrayOutputStream buffer              = new ByteArrayOutputStream();

   private Connector             connector;

   public MSKDevice(Connector connector) {
      this.connector = connector;
      init();

   }

   private void init() {
      try {
         LOG.debug("GPIO setup for MSK device");

         // Default transfer modes for SPI
         spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiMode.MODE_0);
         gpio = GpioFactory.getInstance();

         // Interrupt setup
         gpioDio = gpio.provisionDigitalInputPin(dio, PinPullResistance.PULL_DOWN);
         gpioDio.setShutdownOptions(true);
         gpioDio.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                //LOG.info("State change: " + event.getEventType().toString() + " " +  event.getEdge().getName());

               if (event.getEdge() == PinEdge.RISING) {
                  if (tx) {
                     // Clear RX and put back into RX mode (stops transmitting after packet is sent)
                     //try { Thread.sleep(1); } catch(Throwable e) { }
                     writeRegister(REG_OP_MODE, 0x04);
                     writeRegister(REG_OP_MODE, 0x05);
                     tx = false;

                  } else {
                     // RX mode - payloadReady
                     int tf = readRegister(0x3f);
//LOG.info("INT REG: " + Integer.toBinaryString(tf));
                     // check crc flag
                     boolean crcOk = (tf & 0x2) == 0x2;
                     if (!crcOk) {
                        LOG.info("Ignoring packet with bad CRC");
                     }

                     checkBuffer(true, tf);
                     if (crcOk) {
                        getMessage();
                     } else {
                        getMessage();
                        resetPacket();
                     }
                  }
               }

            }
         });

         // Select pin
         gpioSS = gpio.provisionDigitalOutputPin(ss, PinState.HIGH);
         gpioSS.setShutdownOptions(true, PinState.LOW);
         gpioSS.high();

      } catch (IOException e) {
         LOG.error(e.getMessage(), e);
      }

      // Get the device version
      int version = readRegister(REG_VERSION);
      if (version == 0x12) {
         // Setup a SX1276 into long range, always rx - we dont care about power usage
         LOG.info("MSK Device recognised as a SX1276");
         writeRegister(REG_OP_MODE, MODE_SLEEP);
         
       
          setChannelSX1276(SX1276_DEFAULT_FREQ);

         // Rx startup regrxcfg
        // writeRegister(0x0d, 0b00011000); // set opts
         writeRegister(0x0d, 0b00011111); // set opts

         // Defaults to 19.2kbaud
         // writeRegister(0x03, 0x83); // bitrate 7:0
         // writeRegister(0x02, 0x06); // bitrate 15:8

         // Defaults to 12.5kbaud
        // writeRegister(0x03, 0x00); // bitrate 7:0
        // writeRegister(0x02, 0x0A); // bitrate 15:8

         // Defaults to 9k6kbaud
        //  writeRegister(0x03, 0x05); // bitrate 7:0
        //  writeRegister(0x02, 0x0D); // bitrate 15:8

         // 1200
         // writeRegister(0x03, 0x2b); // bitrate 7:0
         // writeRegister(0x02, 0x68); // bitrate 15:8
         
         // 2400
         //writeRegister(0x03, 0x15); // bitrate 7:0
         //writeRegister(0x02, 0x34); // bitrate 15:8
         
         // 4800
         writeRegister(0x03, 0x0b); // bitrate 7:0
         writeRegister(0x02, 0x1a); // bitrate 15:8

         // regfdev (default 5khz deviation)
         // setFrequencyDeviation(10.4); // 10.4kHz
         setFrequencyDeviation(2.6); // 3kHz

         // regpaRamp
         //writeRegister(0x0A, 0b00001111);
         writeRegister(0x0A, 0b00001111);

         writeRegister(REG_PAYLOAD_LENGTH, 0xff);

         // Gain
         writeRegister(REG_LNA, LNA_MAX_GAIN);

         // mant exp bw
         // 10b / 24 5 10.4
         //// 01b / 20 5 12.5
         //writeRegister(0x12, 0b00010101); // settings for 10.4khz
         //writeRegister(0x13, 0b00001101); // AFC settings. 12.5kHz
         
         // mant exp bw  2.5
         // 10b / 24 7 2.6
         // 01b / 20 7 3.1
         // 00b / 16 7 3.9
         writeRegister(0x12, 0b00001111); // settings for 10.4khz
         writeRegister(0x13, 0b00010110); // AFC settings. 12.5kHz

         // old works at 2.6k
         //writeRegister(0x12, 0b00001111); // settings for 10.4khz
         //writeRegister(0x13, 0b00010111); // AFC settings. 12.5kHz

         
         writeRegister(0x26,24); // preamble length
         //writeRegister(0x1f, 0b10101000);

         writeRegister(0x0B, 0b00111011); // reduce overcurrent protection

         writeRegister(REG_DIO_MAPPING_1, 0x10);
         // writeRegister(REG_MAX_PAYLOAD_LENGTH, 0xff);

         writeRegister(REG_PA_CONFIG, PA_BOOST | (17 - 2)); // 10 = power level

         // writeRegister(0x0A, 0b01001100); // PA Ramp

         writeRegister(REG_4D_PA_DAC, 0x84);
         // writeRegister(REG_LNA, readRegister(REG_LNA) | 0x03);

         writeRegister(0x33, 0b00110101); // Node Address
         writeRegister(0x30, 0b11011000); // Setup for packet mode (not direct transmitter keying)
         writeRegister(0x31, 0b01000000); // packet mode
         writeRegister(0x35, 0b10010000); // fifo threshold (32 bytes)
         writeRegister(0x27, 0b00110110); // sync word, preamble polarity, autorestartrxmode
         
         
         writeRegister(0x28, 0xff);
         writeRegister(0x29, 0x00);
         writeRegister(0x2a, 0x55);
         writeRegister(0x2b, 0xaa);
         writeRegister(0x2c, 0x00);
         writeRegister(0x2d, 0xff);



         writeRegister(0x1a, 0b00000001);  

         
         // manual use.
         writeRegister(REG_OP_MODE, MODE_SLEEP);
         sleep(150);
         writeRegister(REG_OP_MODE, MODE_STANDBY);
         sleep(150);
         writeRegister(0x24,  0b00001000);

         sleep(500);
         
         writeRegister(REG_OP_MODE, 0x04);
         sleep(350);
         writeRegister(REG_OP_MODE, 0x05);
         sleep(150);
         //writeRegister(0x0d, 0b00111000); // set opts
         writeRegister(0x0d, 0b00111111); // set opts

         sleep(150);

      } else {
         LOG.error("Unknown device found! version: " + Integer.toString(version, 16));
      }

      Timer test = new Timer();
      test.schedule(new TimerTask() {
         public void run() {

            if (!tx) {
               int tf = readRegister(0x3f);
               //LOG.info("Register 3F:   " + Integer.toBinaryString(tf));
               checkBuffer(false, tf);
            }

         }
      }, 1000, 3);

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

   private void getMessage() {

      try {
         if (buffer.size() > 0) {

            // Full packet received

            // Send to our packet engine
            try {
               byte[] decompressed = Tools.decompress(buffer.toByteArray());
               LOG.info("MSK Rx payload(" + buffer.size() + "): " + Tools.byteArrayToHexString(buffer.toByteArray()));
               connector.getPacketEngine().receivePacket(decompressed);
               
               // Post the event for all and sundry
               ServerBus.INSTANCE.post(new RxRFPacket(connector, buffer.toByteArray(), System.currentTimeMillis()));
            } catch (EOFException e) {
               LOG.info("MSK Rx corrupt payload(" + buffer.size() + "): " + Tools.byteArrayToHexString(buffer.toByteArray()));
            }
         }
      } finally {

         resetPacket();
      }
   }

   /**
    * Send a message packet
    */
   public void sendMessage(byte[] compressedData) {
      ServerBus.INSTANCE.post(new TxRFPacket(connector, compressedData));
      sendPacket(compressedData);
   }

   private void sendPacket(byte[] message) {
      tx = true;
      LOG.info("MSK Tx payload(" + message.length + ")   "+ Tools.byteArrayToHexString(message));

      // Standby mode to prevent any further rx or interrupts changing fifo
      writeRegister(REG_OP_MODE, MODE_STANDBY);
      writeRegister(REG_OP_MODE, MODE_TX);
      writeRegister(REG_FIFO, (int) message.length); // fill fifo
      for (byte b : message) {
         while ((readRegister(0x3f) & 0x80) > 0) {
            Thread.yield();
         }
         writeRegister(REG_FIFO, (int) b); // fill fifo

      }
   }

   private int     packetLength = 0;
   private boolean lengthRead   = false;

   public synchronized void checkBuffer(boolean completed, int x) {
      if ((x & 0x20) != 0 || completed) {
         while ((x & 0x40) == 0) {
            int data = readRegister(REG_FIFO);
            if (!lengthRead) {
               packetLength = data;
               lengthRead = true;
            } else {
               buffer.write((byte) data);
            }
            x = readRegister(0x3f);
            try { Thread.sleep(1); } catch(Throwable e) { }
//            if ((x & 0x4) != 0 || (x & 0x2) != 0) {
        //       LOG.info("Packet flag as complete: "+Integer.toBinaryString(x));
//            }
 
         }
         if ((x & 0x4) != 0) {
            getMessage();
            resetPacket();
         }
      }
   }

   public void resetPacket() {

      buffer.reset();
      lengthRead = false;
      packetLength = 0;
   }

   public final void sleep(final long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
      }
   }

}