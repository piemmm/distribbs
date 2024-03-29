package org.prowl.distribbs.utils;

import com.pi4j.io.gpio.*;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.system.SystemInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Single class to access the hardware
 */
public enum Hardware {

    INSTANCE;

    private final Log LOG = LogFactory.getLog("Hardware");
    private final Semaphore spiLock = new Semaphore(1, true);
    private float MAX_CPU_TEMP = 75f;
    private SpiDevice spi;

    private Pin dio0 = RaspiPin.GPIO_07;
    private Pin dio1 = RaspiPin.GPIO_03;

    private Pin ss0 = RaspiPin.GPIO_06;
    private Pin ss1 = RaspiPin.GPIO_02;

    private Pin reset = RaspiPin.GPIO_00;

    private Pin fan1 = RaspiPin.GPIO_27;
    private Pin fan2 = RaspiPin.GPIO_24;

    private GpioController gpio;
    private GpioPinDigitalInput gpioDio0;
    private GpioPinDigitalInput gpioDio1;
    private GpioPinDigitalOutput gpioSS0;
    private GpioPinDigitalOutput gpioSS1;
    private GpioPinDigitalOutput gpioRst;

    private GpioPinDigitalOutput gpioFan1;
    private GpioPinDigitalOutput gpioFan2;

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
            gpioSS0.setShutdownOptions(true, PinState.HIGH);
            gpioSS0.high();

            // Select pin 1
            gpioSS1 = gpio.provisionDigitalOutputPin(ss1, PinState.HIGH);
            gpioSS1.setShutdownOptions(true, PinState.HIGH);
            gpioSS1.high();

            // Fan 1, default ON until temp code turns off
            gpioFan1 = gpio.provisionDigitalOutputPin(fan1, PinState.HIGH);
            gpioFan1.setShutdownOptions(true, PinState.HIGH);
            gpioFan1.high();

            // Fan 2, default ON until temp code turns off
            gpioFan2 = gpio.provisionDigitalOutputPin(fan2, PinState.HIGH);
            gpioFan2.setShutdownOptions(true, PinState.HIGH);
            gpioFan2.high();

            // Reset (default being reset until we're ready). This also is commoned with the
            // RF modules reset pin.
            gpioRst = gpio.provisionDigitalOutputPin(reset, PinState.HIGH);
            gpioRst.setShutdownOptions(true, PinState.LOW); // If the VM exits or something quits us, we make sure the SX modules can't
            // transmit
            gpioRst.low();
            resetAll();

            makeThermalMonitor();

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    public I2CDevice getI2CDevice(int addr) throws IOException, I2CFactory.UnsupportedBusNumberException {
        return I2CFactory.getInstance(1).getDevice(addr);
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

    /**
     * Simple fan controller for making sure the pi cpu doesn't get too close to
     * it's thermal limits.
     */
    public void makeThermalMonitor() {

        Thread thread = new Thread() {
            public void run() {
                LOG.info("Thermal monitor starting");
                while (true) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                    try {
                        float currentTemp = SystemInfo.getCpuTemperature();
                        //LOG.debug("CPU thermals:" + currentTemp);
                        if (currentTemp > MAX_CPU_TEMP) {
                            gpioFan1.high();
                            gpioFan2.high();
                        } else if (currentTemp < MAX_CPU_TEMP - 5) {
                            gpioFan1.low();
                            gpioFan2.low();
                        }
                    } catch (UnsupportedOperationException e) {
                        LOG.warn("CPU Does not support temperature measurement:" + e.getMessage());
                        break;
                    } catch (IOException e) {
                        LOG.warn("CPU Does not support temperature measurement:" + e.getMessage());
                        break;
                    } catch (Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                LOG.info("Thermal monitor exiting");
            }
        };

        thread.start();

    }

}
