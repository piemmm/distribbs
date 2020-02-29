package org.prowl.distribbs.utils;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * Single class to access the hardware
 */
public enum Hardware {

   INSTANCE;

   private final Log            LOG     = LogFactory.getLog("RxRFPacket");

   private final Semaphore      spiLock = new Semaphore(1, true);
   private SpiDevice            spi;

   private Pin                  dio0    = RaspiPin.GPIO_07;
   private Pin                  dio1    = RaspiPin.GPIO_03;

   private Pin                  ss0     = RaspiPin.GPIO_06;
   private Pin                  ss1     = RaspiPin.GPIO_02;

   private GpioController       gpio;
   private GpioPinDigitalInput  gpioDio0;
   private GpioPinDigitalInput  gpioDio1;
   private GpioPinDigitalOutput gpioSS0;
   private GpioPinDigitalOutput gpioSS1;

   private Hardware() {
      try {
         spi = SpiFactory.getInstance(SpiChannel.CS0, 2_000_000, SpiMode.MODE_0);

         gpio = GpioFactory.getInstance();

         // Interrupt setup 0
         gpioDio0 = gpio.provisionDigitalInputPin(dio0, PinPullResistance.PULL_DOWN);
         gpioDio0.setShutdownOptions(true);

         // Interrupt setup 1
         gpioDio1 = gpio.provisionDigitalInputPin(dio1, PinPullResistance.PULL_DOWN);
         gpioDio1.setShutdownOptions(true);

         // Select pin 0
         gpioSS0 = gpio.provisionDigitalOutputPin(ss0, PinState.HIGH);
         gpioSS0.setShutdownOptions(true, PinState.LOW);
         gpioSS0.high();

         // Select pin 1
         gpioSS1 = gpio.provisionDigitalOutputPin(ss1, PinState.HIGH);
         gpioSS1.setShutdownOptions(true, PinState.LOW);
         gpioSS1.high();

      } catch (IOException e) {
         LOG.error(e.getMessage(), e);
      }

   }

   public final Semaphore getSPILock() {
      return spiLock;
   }

   public final SpiDevice getSPI() {
      return spi;
   }

   public GpioPinDigitalOutput getGpioSS0() {
      return gpioSS0;
   }

   public GpioPinDigitalOutput getGpioSS1() {
      return gpioSS1;
   }

   
   
}
