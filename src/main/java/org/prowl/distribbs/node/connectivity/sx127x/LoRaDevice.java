package org.prowl.distribbs.node.connectivity.sx127x;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;

/**
 * LoRa device interface based on information and examples in the semtech
 * datasheet https://www.mouser.com/datasheet/2/761/sx1276-1278113.pdf
 * <p>
 * Device config is setup to ignore LoRaWAN (we just use LoRa) and not be
 * concerned with power usage as we are more after performance and range.
 */
public class LoRaDevice implements Device {

    private static final int REG_FIFO = 0x00;
    private static final int REG_OP_MODE = 0x01;
    private static final int REG_OCP = 0x0b;
    private static final int REG_LNA = 0x0c;
    private static final int REG_FIFO_ADDR_PTR = 0x0D;
    private static final int REG_FIFO_TX_BASE_AD = 0x0E;
    private static final int REG_FIFO_RX_BASE_AD = 0x0F;
    private static final int REG_PA_CONFIG = 0x09;
    private static final int REG_FIFO_RX_CURRENT_ADDR = 0x10;
    private static final int REG_IRQ_FLAGS = 0x12;
    private static final int REG_RX_NB_BYTES = 0x13;
    private static final int REG_MODEM_CONFIG1 = 0x1D;
    private static final int REG_MODEM_CONFIG2 = 0x1E;
    private static final int REG_PAYLOAD_LENGTH = 0x22;
    private static final int REG_MAX_PAYLOAD_LENGTH = 0x23;
    private static final int REG_MODEM_CONFIG3 = 0x26;
    private static final int REG_DETECTION_OPTIMIZE = 0x31;
    private static final int REG_DETECTION_THRESHOLD = 0x37;
    private static final int REG_SYNC_WORD = 0x39;
    private static final int REG_VERSION = 0x42;
    private static final int REG_4D_PA_DAC = 0x4d;
    private static final int MODE_LONG_RANGE_MODE = 0x80;
    private static final int MODE_SLEEP = 0x00;
    private static final int MODE_STANDBY = 0x01;
    private static final int MODE_TX = 0x03;
    private static final int MODE_RX_CONTINUOUS = 0x05;
    private static final int REG_FRF_MSB = 0x06;
    private static final int REG_FRF_MID = 0x07;
    private static final int REG_FRF_LSB = 0x08;
    private static final int LNA_MAX_GAIN = 0b00100011;
    private static final int LNA_OFF_GAIN = 0b00000000;
    private static final int LNA_MIN_GAIN = 0x11000000;
    private static final int PA_DAC_DISABLE = 0x04;
    private static final int PA_DAC_ENABLE = 0x07;                           // 0x07
    private static final int IRQ_TX_DONE_MASK = 0x08;
    private static final int IRQ_PAYLOAD_CRC_ERROR_MASK = 0x20;
    private static final int IRQ_RX_DONE_MASK = 0x40;
    private static final int PA_BOOST = 0x80;

    private static final double FREQ_STEP = 61.03515625d;
    private static final int SX1276_DEFAULT_FREQ = 868100000;                      // this should be 433 after testing

    private static final Log LOG = LogFactory.getLog("LoRaDevice");

    public static SpiDevice spi = null;
    private GpioController gpio;
    private GpioPinDigitalOutput gpioSS;
    private GpioPinDigitalInput gpioDio;
    private Pin dio = RaspiPin.GPIO_07;
    private Pin ss = RaspiPin.GPIO_06;
    private int frequency = SX1276_DEFAULT_FREQ;
    private int deviation = 2600;
    private int baud = 4800;

    private Interface connector;
    private int slot;

    public LoRaDevice(Interface connector, int slot, int frequency, int deviation, int baud) {
        this.connector = connector;
        this.slot = slot;
        this.frequency = frequency;
        this.deviation = deviation;
        this.baud = baud;
        init();
    }

    private void init() {
        try {
            LOG.debug("GPIO setup for LoRa device");

            // Slot 0 = 144, Slot 1 = 433
            if (slot == 1) {
                dio = RaspiPin.GPIO_03;
                ss = RaspiPin.GPIO_02;
            }
            // Default transfer modes for SPI
            spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiMode.MODE_0);
            gpio = GpioFactory.getInstance();

            // Interrupt setup
            gpioDio = gpio.provisionDigitalInputPin(dio, PinPullResistance.PULL_DOWN);
            gpioDio.setShutdownOptions(true);
            gpioDio.addListener(new GpioPinListenerDigital() {
                @Override
                public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                    if (event.getEdge() == PinEdge.RISING) {
                        LOG.debug("LoRa message received");
                        getMessage();
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

        // Get the device version
        int version = readRegister(REG_VERSION);
        if (version == 0x12) {
            // Setup a SX1276 into long range, always rx - we dont care about power usage
            LOG.info("Device recognised as a SX1276");
            writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_SLEEP);
            setChannelSX1276(SX1276_DEFAULT_FREQ);
            writeRegister(REG_SYNC_WORD, 0x12); // LoRa private sync (we don't want lorawan)
            writeRegister(REG_MODEM_CONFIG1, 0b10000010);
            writeRegister(REG_MODEM_CONFIG2, 0b01110100);
            writeRegister(REG_MODEM_CONFIG3, 0x04);
            writeRegister(REG_MAX_PAYLOAD_LENGTH, 0xff);
            writeRegister(REG_PAYLOAD_LENGTH, 0x40);
            writeRegister(REG_FIFO_RX_BASE_AD, 0x00);
            writeRegister(REG_FIFO_TX_BASE_AD, 0x00);
            writeRegister(REG_FIFO_ADDR_PTR, 0x00);
            writeRegister(REG_PA_CONFIG, PA_BOOST | (10 - 2)); // 10 = power level
            writeRegister(REG_OCP, 0x1f); // reduce overcurrent protection
            writeRegister(REG_LNA, LNA_MAX_GAIN);
            writeRegister(REG_4D_PA_DAC, 0x84);

            // spreading factor optimize 6
            int sf = 8; // sf 6
            int bw = 8; // 250k
            int cr = 5; // coding rate
            writeRegister(REG_DETECTION_OPTIMIZE, 0xc5);
            writeRegister(REG_DETECTION_THRESHOLD, 0x0c);
            writeRegister(REG_MODEM_CONFIG1, (readRegister(REG_MODEM_CONFIG1) & 0x0f) | (bw << 4)); // bandwidth
            writeRegister(REG_MODEM_CONFIG1, (readRegister(REG_MODEM_CONFIG1) & 0xf1) | (cr << 1)); // coding rate
            writeRegister(REG_MODEM_CONFIG2, (readRegister(REG_MODEM_CONFIG2) & 0x0f) | ((sf << 4) & 0xf0)); // spreading factor

            // preamble
            // int length = 16;
            // writeRegister(REG_PREAMBLE_MSB, ((int)(length >> 8) && 0xff));
            // writeRegister(REG_PREAMBLE_LSB, (uint8_t)(length >> 0));

            writeRegister(REG_LNA, readRegister(REG_LNA) | 0x03);

//         // 868 sensitivity improve
//         writeRegister(0x36,0x02);
//         writeRegister(0x3a,0x64);
//         
//         //// 433 sensitivity improve
//         //writeRegister(0x36,0x02);
//         //writeRegister(0x3a,0x7F);

            writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_RX_CONTINUOUS);
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
                LOG.debug("LoRa Rx payload: " + Tools.byteArrayToHexString(message) + "  \"" + Tools.textOnly(message) + "\"");
                // Post the event for all and sundry
                ServerBus.INSTANCE.post(new RxRFPacket(connector, message, System.currentTimeMillis(), 0));
            }
        }

    }

    /**
     * Send a message packet via LoRa
     */
    public void sendMessage(TxRFPacket packet) {

        ServerBus.INSTANCE.post(packet);
        LOG.debug("LoRa Tx payload(" + packet.getCompressedPacket().length + ")");
        sendPacket(packet.getCompressedPacket());
    }

    private byte[] getPacket() {
        writeRegister(REG_IRQ_FLAGS, 0x40);

        // Check CRC flag
        if ((readRegister(REG_IRQ_FLAGS) & 0x20) == 0x20) {
            LOG.debug("Ignoring packet with bad CRC");
            writeRegister(REG_IRQ_FLAGS, 0x20);
            writeRegister(REG_FIFO_ADDR_PTR, 0x00);
            return null;
        }

        writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_STANDBY);
        int currentAddr = readRegister(REG_FIFO_RX_CURRENT_ADDR);
        int receivedCount = (readRegister(REG_RX_NB_BYTES) & 0xFF);
        writeRegister(REG_FIFO_ADDR_PTR, currentAddr);
        byte[] res = new byte[receivedCount];
        for (int i = 0; i < receivedCount; i++) {
            int c = readRegister(REG_FIFO) & 0xff;
            res[i] = (byte) c;
        }
        writeRegister(REG_FIFO_ADDR_PTR, 0x00);
        writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_RX_CONTINUOUS);

        return res;
    }

    private void sendPacket(byte[] message) {
        // Standby mode to prevent any further rx or interrupts changing fifo
        writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_STANDBY);
        writeRegister(LoRaDevice.REG_FIFO_ADDR_PTR, 0x0);
        for (byte b : message) {
            writeRegister(REG_FIFO, (int) b); // fill fifo
        }
        writeRegister(REG_PAYLOAD_LENGTH, message.length);
        // Now send
        writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_TX);
        // Mandatory wait for chip to setup
        sleep(150);
        // Now wait until TX empty
        while ((readRegister(REG_IRQ_FLAGS) & IRQ_TX_DONE_MASK) == 0) {
            sleep(100);
        }
        // All sent. Now clear flags, reset fifo and set to RX again.
        writeRegister(REG_IRQ_FLAGS, IRQ_TX_DONE_MASK);
        writeRegister(REG_FIFO_ADDR_PTR, 0x00);
        writeRegister(REG_OP_MODE, MODE_LONG_RANGE_MODE | MODE_RX_CONTINUOUS);
    }

    public boolean isTransmitting() {
        if ((readRegister(REG_OP_MODE) & MODE_TX) == MODE_TX) {
            return true;
        }
        if ((readRegister(REG_IRQ_FLAGS) & IRQ_TX_DONE_MASK) == 0) {
            // clear IRQ's
            writeRegister(REG_IRQ_FLAGS, IRQ_TX_DONE_MASK);
        }
        return false;
    }

    public final void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public double getNoiseFloor() {
        return 0;
    }

    public double getRSSI() {
        return 0;
    }

    @Override
    public int setFrequency(int frequencyHz) {
        return 0;
    }

    @Override
    public double setDeviation(double deviationHz) {
        return 0;
    }

    @Override
    public int setAFCFilter(int afcHz) {
        return 0;
    }

    @Override
    public int setDemodFilter(int demodHz) {
        return 0;
    }

    @Override
    public int setBaud(int baud) {
        return 0;
    }

}