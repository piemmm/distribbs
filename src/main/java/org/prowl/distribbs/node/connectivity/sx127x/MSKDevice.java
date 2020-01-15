package org.prowl.distribbs.node.connectivity.sx127x;

import java.io.IOException;

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

   private static final int     REG_FIFO                 = 0x00;
   private static final int     REG_OP_MODE              = 0x01;
   private static final int     REG_OCP                  = 0x0b;
   private static final int     REG_LNA                  = 0x0c;
   private static final int     REG_FIFO_ADDR_PTR        = 0x0D;
   private static final int     REG_PA_CONFIG            = 0x09;
   private static final int     REG_FIFO_RX_CURRENT_ADDR = 0x10;
   private static final int     REG_IRQ_FLAGS            = 0x3e;
   private static final int     REG_IRQ_FLAGS2           = 0x3f;
   private static final int     REG_RX_NB_BYTES          = 0x13;

   private static final int     REG_PAYLOAD_LENGTH       = 0x22;
   private static final int     REG_MAX_PAYLOAD_LENGTH   = 0x23;
   private static final int     REG_SYNC_WORD            = 0x39;
   private static final int     REG_DIO_MAPPING_1        = 0x40;
   private static final int     REG_DIO_MAPPING_2        = 0x41;
   private static final int     REG_VERSION              = 0x42;
   private static final int     REG_4D_PA_DAC            = 0x4d;
   private static final int     MODE_SLEEP               = 0x00;
   private static final int     MODE_STANDBY             = 0x01;
   private static final int     MODE_TX                  = 0x03;
   private static final int     MODE_RX_CONTINUOUS       = 0x05;
   private static final int     REG_FRF_MSB              = 0x06;
   private static final int     REG_FRF_MID              = 0x07;
   private static final int     REG_FRF_LSB              = 0x08;
   private static final int     LNA_MAX_GAIN             = 0b00100011;

   private static final int     IRQ_TX_DONE_MASK         = 0x8;
   private static final int     PA_BOOST                 = 0x80;

   private static final double  FREQ_STEP                = 61.03515625d;
   private static final int     SX1276_DEFAULT_FREQ      = 868100000;

   private static final Log     LOG                      = LogFactory.getLog("MSKDevice");

   public static SpiDevice      spi                      = null;
   private GpioController       gpio;
   private GpioPinDigitalOutput gpioRst;
   private GpioPinDigitalOutput gpioSS;
   private GpioPinDigitalInput  gpioDio;
   private Pin                  reset                    = RaspiPin.GPIO_00;
   private Pin                  dio                      = RaspiPin.GPIO_07;
   private Pin                  ss                       = RaspiPin.GPIO_06;

   private boolean              tx                       = false;                         // True when in TX.

   public MSKDevice() {
      init();
   }

   private void init() {
      try {
         LOG.debug("GPIO setup for MSK device");

         // Default transfer modes for SPI
         spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiMode.MODE_0);
         gpio = GpioFactory.getInstance();

         // Reset (default being reset until we're ready)
         gpioRst = gpio.provisionDigitalOutputPin(reset, PinState.HIGH);
         gpioRst.setShutdownOptions(true, PinState.LOW);
         gpioRst.low();

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
                     // RX mode
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
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      // Reset the chip
      resetLoRa();

      // Get the device version
      int version = readRegister(REG_VERSION);
      if (version == 0x12) {
         // Setup a SX1276 into long range, always rx - we dont care about power usage
         LOG.info("Device recognised as a SX1276");
         writeRegister(REG_OP_MODE, MODE_SLEEP);
         setChannelSX1276(SX1276_DEFAULT_FREQ);

         // Defaults to 19.2kbaud
         // writeRegister(0x03, 0x83); // bitrate 7:0
         // writeRegister(0x02, 0x06); // bitrate 15:8

         // 1200
         writeRegister(0x03, 0x2b); // bitrate 7:0
         writeRegister(0x02, 0x68); // bitrate 15:8

         // regfdev (default 5khz deviation)

         // regpaRamp
         writeRegister(0x0A, 0b00001001);

         // Preamble (3)

         // Rx startup regrxcfg
         // writeRegister(, ) rxtrigger 2:0

         writeRegister(REG_OCP, 0x1f); // reduce overcurrent protection

         // writeRegister(REG_LNA, LNA_MAX_GAIN);

         writeRegister(REG_DIO_MAPPING_1, 0x0);
         writeRegister(REG_MAX_PAYLOAD_LENGTH, 0xff);
         writeRegister(REG_PAYLOAD_LENGTH, 0xff);

         writeRegister(REG_PA_CONFIG, PA_BOOST | (10 - 2)); // 10 = power level

         writeRegister(REG_4D_PA_DAC, 0x84);
         // writeRegister(REG_LNA, readRegister(REG_LNA) | 0x03);

         // Setup for packet mode (not direct transmitter keying)
         writeRegister(0x30, 0b10000000);
         writeRegister(0x31, 0b01000000);

         writeRegister(0x27, 0b01000000); // sync word, preamble polarity, autorestartrxmode
         clearIRQ();
         writeRegister(REG_OP_MODE, MODE_RX_CONTINUOUS);
      } else {
         LOG.error("Unknown device found! version: " + Integer.toString(version, 16));
      }

   }

   public void setChannelSX1276(int freq) {
      freq = (int) ((double) freq / (double) FREQ_STEP);
      writeRegister(REG_FRF_MSB, (int) ((freq >> 16) & 0xFF));
      writeRegister(REG_FRF_MID, (int) ((freq >> 8) & 0xFF));
      writeRegister(REG_FRF_LSB, (int) (freq & 0xFF));
   }

   public void resetLoRa() {
      LOG.debug("LoRa device reset");
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
      }
      gpioRst.low();
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
      }
      gpioRst.high();
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
      }
   }

   private int readRegister(int addr) {
      byte spibuf[] = new byte[2];
      int res = 0x00;
      spibuf[0] = (byte) (addr);
      spibuf[1] = 0x00;
      disableSS();
      try {
         byte[] result = spi.write(spibuf);
         res = result[1];
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      enableSS();
      return res;
   }

   private void writeRegister(int addr, int value) {
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
   }

   private void enableSS() {
      gpioSS.high();
   }

   private void disableSS() {
      gpioSS.low();
   }

   private void getMessage() {

      if (gpioDio.isHigh()) {
         byte[] message = getPacket();
         if (message != null) {
            LOG.info("MSK Rx payload: " + Tools.byteArrayToHexString(message) + "  \"" + Tools.textOnly(message) + "\"");
         }
      }

   }

   /**
    * Send a message packet via LoRa
    */
   public void sendMessage(byte[] data) {
      sendPacket(data);
   }

   private byte[] getPacket() {
      clearIRQ();
      // Check CRC flag
      if ((readRegister(REG_IRQ_FLAGS) & 0x2) == 0x2) {
         LOG.debug("Ignoring packet with bad CRC");
         writeRegister(REG_IRQ_FLAGS, 0x02);
         // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
         return null;
      }

      writeRegister(REG_OP_MODE, MODE_STANDBY);
      int currentAddr = readRegister(REG_FIFO_RX_CURRENT_ADDR);
      int receivedCount = (readRegister(REG_RX_NB_BYTES) & 0xFF);
      writeRegister(REG_FIFO_ADDR_PTR, currentAddr);
      byte[] res = new byte[receivedCount];
      for (int i = 0; i < receivedCount; i++) {
         int c = readRegister(REG_FIFO) & 0xff;
         res[i] = (byte) c;
      }
      // writeRegister(REG_FIFO_ADDR_PTR, 0x00);
      clearIRQ();
      writeRegister(REG_OP_MODE, MODE_RX_CONTINUOUS);

      return res;
   }

   public void clearIRQ() {
      writeRegister(REG_IRQ_FLAGS, 0b11111111);
      writeRegister(REG_IRQ_FLAGS2, 0b11111111);
   }

   private void sendPacket(byte[] message) {
      // Standby mode to prevent any further rx or interrupts changing fifo
      writeRegister(REG_OP_MODE, MODE_STANDBY);
      clearIRQ();
      for (byte b : message) {
         writeRegister(REG_FIFO, (int) b); // fill fifo
      }
      writeRegister(REG_PAYLOAD_LENGTH, message.length);
      // Now send
      tx = true;
      writeRegister(REG_OP_MODE, MODE_TX);

   }

   public boolean isTransmitting() {
      if ((readRegister(REG_OP_MODE) & MODE_TX) == MODE_TX) {
         return true;
      }
      return false;
   }

   public final void sleep(final long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
      }
   }

}