package org.prowl.distribbs.lcd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

/**
 * US2066 LCD driver from newhaven (I2C setup).
 * 
 * This may also work for the SSD1311 controller as well, but has not been
 * tested.
 */
public class US2066 {

   private static final Log     LOG   = LogFactory.getLog("US2066");

   private GpioController       gpio;
   private I2CDevice            i2c;
   private Pin                  reset = RaspiPin.GPIO_00;
   private GpioPinDigitalOutput gpioRst;

   public US2066() {
      init();
   }

   public void init() {

      try {

         // Reset (default being reset until we're ready). This also is commoned with the RF modules reset pin.
         gpio = GpioFactory.getInstance();
         gpioRst = gpio.provisionDigitalOutputPin(reset, PinState.HIGH);
         gpioRst.setShutdownOptions(true, PinState.LOW); // If the VM exits or something quits us, we make sure the SX modules can't transmit
         gpioRst.low(); 
         resetAll();
         
         i2c = I2CFactory.getInstance(1).getDevice(0x3D);

         delay(100);
         // Init the display
         i2c.write(0x00, (byte) 0x2A);
         i2c.write(0x00, (byte) 0x71);
         i2c.write(0x40, (byte) 0x00);
         i2c.write(0x00, (byte) 0x28);
         i2c.write(0x00, (byte) 0x08);
         i2c.write(0x00, (byte) 0x2a);
         i2c.write(0x00, (byte) 0x79);
         i2c.write(0x00, (byte) 0xd5);
         i2c.write(0x00, (byte) 0x70);
         i2c.write(0x00, (byte) 0x78);
         i2c.write(0x00, (byte) 0x09);
         i2c.write(0x00, (byte) 0x06);
         i2c.write(0x00, (byte) 0x72);
         i2c.write(0x40, (byte) 0x00);
         i2c.write(0x00, (byte) 0x2a);
         i2c.write(0x00, (byte) 0x79);
         i2c.write(0x00, (byte) 0xda);
         i2c.write(0x00, (byte) 0x10);
         i2c.write(0x00, (byte) 0xdc);
         i2c.write(0x00, (byte) 0x00);
         i2c.write(0x00, (byte) 0x81);
         i2c.write(0x00, (byte) 0x7f);
         i2c.write(0x00, (byte) 0xd9);
         i2c.write(0x00, (byte) 0xf1);
         i2c.write(0x00, (byte) 0xdb);
         i2c.write(0x00, (byte) 0x40);
         i2c.write(0x00, (byte) 0x78);

         // Direction
         i2c.write(0x00, (byte) 0x2a);
         i2c.write(0x00, (byte) 0x05);

         i2c.write(0x00, (byte) 0x28);
         i2c.write(0x00, (byte) 0x01);

         delay(20);

         // Home starts at 0x40 (0x80 - ddram addr set + 0x40 ddram addr)
         i2c.write(0x00, (byte) 0xC0);
         delay(100);
         i2c.write(0x00, (byte) 0x0C);
         writeText("Starting up....", "");
         delay(250);
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
   }

   /**
    * Write 2 lines of text to the output. We avoid clear() so we don't get any
    * flicker. instead we just overwrite all the characters with their new values.
    * 
    * @param line1
    * @param line2
    */
   public void writeText(String line1, String line2) {
      try {
         home();
         byte[] b1 = line1.getBytes();
         for (int i = 0; i < 20; i++) {
            if (i < b1.length) {
               i2c.write(0x40, b1[i]);
            } else {
               i2c.write(0x40, (byte) 0x20);
            }
         }
         // Next line - we will already be at the right place
         byte[] b2 = line2.getBytes();
         for (int i = 0; i < 20; i++) {
            if (i < b2.length) {
               i2c.write(0x40, b2[i]);
            } else {
               i2c.write(0x40, (byte) 0x20);
            }
         }
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
   }

   public void clear() {
      try {
         i2c.write(0x00, (byte) 0x01);
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
      home();
   }

   public void home() {
      try {
         i2c.write(0x00, (byte) 0xC0);
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }

   }

   public void delay(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException e) {
      }
   }
   
   public void resetAll() {
      LOG.debug("Reset issued to devices");
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
      }
      gpioRst.low();
      try {
         Thread.sleep(150);
      } catch (InterruptedException e) {
      }
      gpioRst.high();
      try {
         Thread.sleep(150);
      } catch (InterruptedException e) {
      }
   }

}
