package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
   private static final int      SX1276_DEFAULT_FREQ = 145950000;

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

   public MSKDevice() {
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
               LOG.info("State change: " + event.getEventType().toString() + "  " + event.getEdge().getName());

               if (event.getEdge() == PinEdge.RISING) {
                  if (tx) {
                     // Clear RX and put back into RX mode (stops transmitting after packet is sent)
                     clearIRQ();
                     writeRegister(REG_OP_MODE, MODE_RX_CONTINUOUS);
                     tx = false;

                  } else {
                     // RX mode - payloadReady
                     int tf = readRegister(0x3f);
                     checkBuffer(true, tf);
                     getMessage();
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
         writeRegister(0x0d, 0b00011000); // set opts

         // writeRegister(0x0f,0b)

         // Defaults to 19.2kbaud
         // writeRegister(0x03, 0x83); // bitrate 7:0
         // writeRegister(0x02, 0x06); // bitrate 15:8

         // Defaults to 9k6kbaud
         // writeRegister(0x03, 0x0D); // bitrate 7:0
         // writeRegister(0x02, 0x05); // bitrate 15:8

         // 1200
         // writeRegister(0x03, 0x2b); // bitrate 7:0
         // writeRegister(0x02, 0x68); // bitrate 15:8

         // 2400
         writeRegister(0x03, 0x34); // bitrate 7:0
         writeRegister(0x02, 0x15); // bitrate 15:8

         // 4800
         // writeRegister(0x03, 0x1a); // bitrate 7:0
         // writeRegister(0x02, 0x0b); // bitrate 15:8

         // regfdev (default 5khz deviation)
         // setFrequencyDeviation(10.4); // 10.4kHz
         setFrequencyDeviation(10); // 10.4kHz

         // regpaRamp
         writeRegister(0x0A, 0b00001111);

         writeRegister(REG_PAYLOAD_LENGTH, 0xff);

         // Gain
         writeRegister(REG_LNA, LNA_MAX_GAIN);

         // mant exp bw
         // 10b / 24 5 10.4
         // 01b / 20 5 12.5
         writeRegister(0x12, 0b00001101); // settings for 10.4khz
         writeRegister(0x13, 0b00010101); // AFC settings. 12.5kHz

         // preamble length
         writeRegister(0x26, 2);

         writeRegister(0x0B, 0b00111011); // reduce overcurrent protection

         writeRegister(REG_DIO_MAPPING_1, 0x0);
         // writeRegister(REG_MAX_PAYLOAD_LENGTH, 0xff);

         writeRegister(REG_PA_CONFIG, PA_BOOST | (17 - 2)); // 10 = power level

         // writeRegister(0x0A, 0b01001100); // PA Ramp

         writeRegister(REG_4D_PA_DAC, 0x84);
         // writeRegister(REG_LNA, readRegister(REG_LNA) | 0x03);

         writeRegister(0x33, 0b00110101); // Node Address
         writeRegister(0x30, 0b11011000); // Setup for packet mode (not direct transmitter keying)
         writeRegister(0x31, 0b01000000); // packet mode
         writeRegister(0x35, 0b10010000); // fifo threshold (32 bytes)
         writeRegister(0x27, 0b00010011); // sync word, preamble polarity, autorestartrxmode

         // manual use.
         writeRegister(REG_OP_MODE, MODE_SLEEP);
         sleep(150);
         clearIRQ();
         writeRegister(REG_OP_MODE, 0x04);
         sleep(350);
         writeRegister(REG_OP_MODE, 0x05);
         sleep(150);
         writeRegister(0x0d, 0b00111000); // set opts

         // writeRegister(0x0d,0b000111110); // set opts and kick into receive mode
         // sleep(150);

         // sequencer use - transmit
//         writeRegister(REG_OP_MODE, MODE_SLEEP);
//         sleep(150);
//         writeRegister(0x36,0b10001011);
//         writeRegister(0x37,0b00100100);

      } else {
         LOG.error("Unknown device found! version: " + Integer.toString(version, 16));
      }

      Timer test = new Timer();
      test.schedule(new TimerTask() {
         public void run() {

            if (!tx) {
               int tf = readRegister(0x3f);
               // LOG.info("RSSI:" + readRegister(0x11)+" "+readRegister(0x3e)+" "+tf+"
               // fifoempty:"+(tf & 0x40)+" thresh:"+(tf & 0x20));

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
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            // TODO Auto-generated catch block
            e.printStackTrace();
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

      if (buffer.size() > 0) {
         // Full packet received
         System.out.println("MSK Rx payload(" + buffer.size() + "): " + Tools.byteArrayToHexString(buffer.toByteArray()) + "  \"" + Tools.textOnly(buffer.toByteArray()) + "\"");

         
         
      }

      resetPacket();

      clearIRQ();

   }

   /**
    * Send a message packet via LoRa
    */
   public void sendMessage(byte[] data) {
      sendPacket(data);
   }

//   private byte[] getPacket() {
//      clearIRQ();
//      // Check CRC flag
//      if ((readRegister(REG_IRQ_FLAGS) & 0x2) == 0x2) {
//         LOG.debug("Ignoring packet with bad CRC");
//         writeRegister(REG_IRQ_FLAGS, 0x02);
//         // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
//         return null;
//      }
//
//      writeRegister(REG_OP_MODE, MODE_STANDBY);
//      //int currentAddr = readRegister(REG_FIFO_RX_CURRENT_ADDR);
//     int receivedCount =  readRegister(REG_FIFO) & 0xff;
//     // writeRegister(REG_FIFO_ADDR_PTR, currentAddr);
//      byte[] res = new byte[receivedCount];
//      for (int i = 0; i < receivedCount; i++) {
//         int c = readRegister(REG_FIFO) & 0xff;
//         res[i] = (byte) c;
//      }
//      // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
//      clearIRQ();
//      writeRegister(REG_OP_MODE, MODE_RX_CONTINUOUS);
//
//      return res;
//   }

   public void clearIRQ() {
//  
      // writeRegister(REG_IRQ_FLAGS, 0b11111111);
      // writeRegister(REG_IRQ_FLAGS2, 0b11111111);
   }

   private void sendPacket(byte[] message) {
      tx = true;

      // Standby mode to prevent any further rx or interrupts changing fifo
      writeRegister(REG_OP_MODE, MODE_STANDBY);
      clearIRQ();
      // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
      // writeRegister(REG_PAYLOAD_LENGTH, message.length+1);

      writeRegister(REG_OP_MODE, MODE_TX);
      writeRegister(REG_FIFO, (int) message.length); // fill fifo
      for (byte b : message) {
         while ((readRegister(0x3f) & 0x80) > 0) {
            Thread.yield();
         }
         writeRegister(REG_FIFO, (int) b); // fill fifo

      }

      // sleep(150);
      // Now send

   }

//   
//   private void sendPacket(byte[] message) {
//      tx = true;
//
//      // Standby mode to prevent any further rx or interrupts changing fifo
//      writeRegister(REG_OP_MODE, MODE_STANDBY);
//      clearIRQ();
//      // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
//
//      writeRegister(REG_FIFO, (int) message.length); // fill fifo
//      for (byte b : message) {
//         writeRegister(REG_FIFO, (int) b); // fill fifo
//      }
//      writeRegister(REG_PAYLOAD_LENGTH, message.length+1);
//
//      sleep(150);
//      // Now send
//      writeRegister(REG_OP_MODE, MODE_TX);
//
//   }
//   

   private int     packetLength = 0;
   private boolean lengthRead   = false;

   public synchronized void checkBuffer(boolean completed, int x) {
      // int x = readRegister(0x3f);
      if ((x & 0x20) != 0 || completed) {
         // LOG.info("ReadRegister:" + x + " " + (x & 0x20));
         while ((x & 0x40) == 0) {
            int data = readRegister(REG_FIFO);
            if (!lengthRead) {
               // LOG.info("packetLength:" + data);
               packetLength = data;
               lengthRead = true;
            } else {
               // LOG.info("byte:" + data + " = "+ ((char)data));

               buffer.write((byte) data);
            }
            x = readRegister(0x3f);
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