package org.prowl.distribbs.node.connectivity.sx127x;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiDevice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.utils.EWMAFilter;
import org.prowl.distribbs.utils.Hardware;
import org.prowl.distribbs.utils.Tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * MSK modulation based on information and examples in the semtech datasheet
 * https://www.mouser.com/datasheet/2/761/sx1276-1278113.pdf
 */
public class FSKDevice implements Device {

    private static final int[][] filters = new int[][]{
            {2600, 16, 7},
            {3100, 8, 7},
            {3900, 0, 7},
            {5200, 16, 6},
            {6300, 8, 6},
            {7800, 0, 6},
            {10400, 16, 5},
            {12500, 8, 5},
            {15600, 0, 5},
            {20800, 16, 4},
            {25000, 8, 4},
            {31300, 0, 4},
            {41700, 16, 3},
            {50000, 8, 3},
            {62500, 0, 3},
            {83300, 16, 2},
            {100000, 8, 2},
            {125000, 0, 2},
            {166700, 16, 1},
            {200000, 8, 1},
            {250000, 0, 1}
    };

    private Log LOG = LogFactory.getLog("MSKDevice");

    public SpiDevice spi = Hardware.INSTANCE.getSPI();
    private GpioPinDigitalOutput gpioSS;

    private double rssi = 0;
    private int frequency = 0;                              // Our tx/rx freq
    private int deviation = 2600;                           // The deviation to use, in Hz
    private int baud = 9600;                           // The baud rate we will transmit at. Faster speeds need more

    private double bufferRssi = 0;
    public double nextHighestRSSIValue = 0;                              // quietest recorded rssi
    public double nextRSSIUpdate = 0;                              // When we next update our rssi
    private long lastSent = 0;                              // Time when we last finished sending a packet
    private Semaphore spiLock = Hardware.INSTANCE.getSPILock();
    private Semaphore trxLock = new Semaphore(1, true);
    private double rssiNoiseFloor = 0;
    private double rssiThreshold = 0;
    private long rxPacketTimeout = 0;
    private EWMAFilter ewmaFilter = new EWMAFilter(0.05f);
    private long lastRSSISample = 0;
    private int slot;

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);

    private SX127x connector;
    private ExecutorService pool = Executors.newFixedThreadPool(5);
    private ExecutorService txpool = Executors.newFixedThreadPool(1);

    public FSKDevice(SX127x connector, int slot, int frequency, int deviation, int baud) {
        LOG = LogFactory.getLog("FSKDevice(" + slot + ")");
        this.connector = connector;
        this.slot = slot;
        this.frequency = frequency;
        this.deviation = deviation;
        this.baud = baud;
        init();
    }

    private void init() {
        LOG.debug("GPIO setup for FSK device");

        if (slot == 0) {
            gpioSS = Hardware.INSTANCE.getGpioSS0();
        } else if (slot == 1) {
            gpioSS = Hardware.INSTANCE.getGpioSS1();

        }

        // Get the device version
        int version = readRegister(0x42);
        if (version == 0x12) {
            // Setup a SX1276, always rx - we dont care about power usage
            LOG.info("FSK Device recognised as a SX1276");
            writeRegister(0x01, 0x0); // Set mode to sleep

            // Set the transmit frequency
            setFrequency(frequency);

            // Rx startup regrxcfg
            writeRegister(0x0d, 0b00011111); // set opts

            // Set the buad rate (1200 to 300kbit)
            setBaud(baud);

            // regfdev (default 5khz deviation)
            setDeviation(deviation / 1000d); // 3kHz

            // Set the filter for the used deviation
            setFilter(deviation);

//         if (baud == 12500) {
//            LOG.info("SEtting for 12.5k");
//            setDemodFilter(6300);
//            setAFCFilter(6300);
//         } else if (baud == 57600) {
//            setDemodFilter(31300);
//            setAFCFilter(41700);  
//         }

            // regpaRamp
            //writeRegister(0x0A, 0b00001111); // (M)FSK

            //if (baud <= 4800) {
            writeRegister(0x0A, 0b00001111); // FSK
            //   setDemodFilter(3900);
            //   setAFCFilter(5200);
            //} else {
            //   writeRegister(0x0A, 0b00101111); // G(M)FSK
            //}

            writeRegister(0x32, 0xff); // Payload length

            // Gain
            writeRegister(0x0c, 0x23); // max gain

            writeRegister(0x26, 24); // preamble length

            writeRegister(0x0B, 0b00111011); // reduce overcurrent protection
            writeRegister(0x40, 0x10); // DIO0 setup

            // Set power options
            writeRegister(0x09, 0b11111111); // 10 = power level
            writeRegister(0x4d, 0b10000111); // PA DAC

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

            writeRegister(0x1a, 0b00010001); // AFC FEI
            writeRegister(0x0e, 0b00000010); // RSSI samples used

            // manual use.
            writeRegister(0x01, 0x0); // Sleep
            sleep(150);
            writeRegister(0x01, 0x01); // Standby
            sleep(150);

            writeRegister(0x3b, 0b11000000); // auto image calibration (and do it *now*)
            sleep(150);

            writeRegister(0x24, 0b00001000);
            sleep(500);
            writeRegister(0x0d, 0b00111111); // set opts
            writeRegister(0x01, 0x04); // FSRx
            sleep(350);
            writeRegister(0x01, 0x05); // RX mode
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
                        while ((buffer.size() > 0 || (readRegister(0x11) / 2d) < rssiThreshold)) {
                            tf = readRegister(0x3f);
                            checkBuffer(false, tf);
                            updateRSSI();

                            if (System.currentTimeMillis() > rxPacketTimeout) {
                                rxPacketTimeout = System.currentTimeMillis() + 2000;
                                resetPacket();
                            }
                        }
                        updateRSSI();
                    } catch (Throwable e) {
                        LOG.error(e.getMessage(), e);
                    } finally {
                        trxLock.release();
                    }
                    try {
                        Thread.sleep(1);
                    } catch (Throwable e) {
                    }
                }
            }

        }, 1000);

    }

    /**
     * Set the transmit frequency
     *
     * @param freq in Hz
     */
    public int setFrequency(int ffreq) {
        int freq = (int) ((double) ffreq / (double) 61.03515625d);
        writeRegister(0x06, (int) ((freq >> 16) & 0xFF));
        writeRegister(0x07, (int) ((freq >> 8) & 0xFF));
        writeRegister(0x08, (int) (freq & 0xFF));
        return ffreq;
    }

    /**
     * Set the amout of deviation to use.
     * <p>
     * Higher bandwidths need more deviation
     *
     * @param freq
     */
    public double setDeviation(double freq) {
        int dev = (int) ((freq * (1 << 19)) / 32000);
        writeRegister(0x04, (dev & 0xFF00) >> 8);
        writeRegister(0x05, (dev & 0x00FF));
        return freq;
    }

    /**
     * Read a register from the device
     *
     * @param addr
     * @return
     */
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

    /**
     * Write to a register on the device
     *
     * @param addr
     * @param value
     */
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

    /**
     * Enable the CS for the device
     */
    private void enableSS() {
        gpioSS.high();
    }

    /**
     * Disable the CS for the device
     */
    private void disableSS() {
        gpioSS.low();
    }

    /**
     * Get a received complete message
     *
     * @param crcOk
     */
    private void getMessage(boolean crcOk) {
        LOG.debug("GetMessage called");
        try {
            writeRegister(0x0c, 0x23); // reset agc to max gain
            //writeRegister(0x1a, 0b00010001); // AFC FEI

            // Full packet received
            if (buffer.size() > 0) {
                // writeRegister(0x01, 0x01); // standby
                // writeRegister(0x01, 0x04); // fsrx
                // writeRegister(0x01, 0x05); // rx
                final byte[] array = buffer.toByteArray();
                final long rxTime = System.currentTimeMillis();
                pool.execute(new Runnable() {
                    public void run() {

                        // Send to our packet engine
                        LOG.info("MSK Rx payload(" + array.length + "): " + Tools.byteArrayToHexString(array));

                        RxRFPacket rxRfPacket = new RxRFPacket(connector, array, rxTime, bufferRssi);
                        if (!crcOk) {
                            rxRfPacket.setCorrupt();
                        }

                        // Process the packet
                        connector.getPacketEngine().receivePacket(rxRfPacket);

                        connector.addRxStats(rxRfPacket.getCompressedByteCount(), rxRfPacket.getUncompressedByteCount());

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
        txpool.execute(new Runnable() {
            public void run() {
                sendPacket(packet.getCompressedPacket());
                ServerBus.INSTANCE.post(packet);
                connector.addTxStats(packet.getCompressedByteCount(), packet.getUncompressedByteCount());
            }
        });
    }

    /**
     * TX a packet
     *
     * @param message
     */
    private void sendPacket(byte[] message) {
        try {
            // Wait until non-busy (so other slots can tx/rx over spi), plus collission
            // managment is here too.
            txDelay();
            while (rssi < rssiThreshold || buffer.size() > 0) {
                try {
                    Thread.sleep(130);
                    // LOG.info("WAIT1:"+ rssi+" < "+rssiThreshold +" || "+buffer.size());
                } catch (Throwable e) {
                }
                checkBuffer(false, readRegister(0x3f));
                updateRSSI();
            }

            trxLock.acquireUninterruptibly();

            // Wait for non busy channel as the lock may have taken a moment
            while (rssi < rssiThreshold) {
                try {
                    Thread.sleep(135);
                    // LOG.info("WAIT2:"+ rssi+" < "+rssiThreshold +" || "+buffer.size());

                } catch (Throwable e) {
                }
                checkBuffer(false, readRegister(0x3f));
                updateRSSI();
            }

            // Standby mode to prevent any further rx or interrupts changing fifo
            writeRegister(0x01, 0x01); // standby
            writeRegister(0x01, 0x03); // transmit
            try {
                Thread.sleep(4);
            } catch (Throwable e) {
            }
            writeRegister(0x00, (int) message.length); // fill fifo
            for (byte b : message) {
                while ((readRegister(0x3f) & 0x80) > 0) {
                }
                writeRegister(0x00, (int) b); // fill fifo
            }

            // Wait for packet to be sent - we don't trust using a DIO0 interrupt for this.
            long timeout = System.currentTimeMillis() + 1500; // 1.5 Second max for badly behaving tx
            while ((readRegister(0x3f) & 0x08) == 0 && System.currentTimeMillis() < timeout) {
                try {
                    Thread.sleep(1);
                } catch (Throwable e) {
                }
            }

        } finally {
            writeRegister(0x01, 0x04); // fsrx
            writeRegister(0x01, 0x05); // rx

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

    private int packetLength = 0;
    private boolean lengthRead = false;

    public synchronized boolean checkBuffer(boolean completed, int x) {

        while ((x & 0x40) == 0) {
            int data = readRegister(0x00);
            // rxPacketTimeout = System.currentTimeMillis() + 1000;
            if (!lengthRead) {
                packetLength = data;
                lengthRead = true;
            } else {
                if (buffer.size() == 0) {
                    bufferRssi = (readRegister(0x11) / 2d);
                }
                LOG.debug("RX:" + Integer.toString(data, 16));
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
                double target = rssiNoiseFloor - 12d;
                if (rssiThreshold == 0) {
                    rssiThreshold = target;
                }
                if (target < rssiThreshold) {
                    rssiThreshold -= 0.05;
                } else {
                    rssiThreshold += 0.05;
                }
                // LOG.info("Threshold:"+rssiThreshold+" target:"+target+"
                // Actual:"+rssiNoiseFloor+" "+buffer.size());
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

    /**
     * Set a baud rate - must be one of the rates specified in the sx1278 datasheet
     */
    public int setBaud(int baudRate) {

        int[][] bauds = new int[][]{
                // Baud, reg 2, reg 3
                {1200, 0x68, 0x2b},
                {2400, 0x34, 0x15},
                {4800, 0x1a, 0x0b},
                {9600, 0x0d, 0x05},
                {12500, 0xa, 0x00},
                {19200, 0x06, 0x83},
                {25000, 0x05, 0x00},
                {32768, 0x03, 0xd1},
                {38400, 0x03, 0x41},
                {50000, 0x80, 0x00},
                {57600, 0x02, 0x2c},
                {76800, 0x01, 0xa1},
                {100000, 0x01, 0x40},
                {115200, 0x01, 0x16},
                {150000, 0x00, 0xd5},
                {153600, 0x00, 0xd0},
                {200000, 0x00, 0xa0},
                {250000, 0x00, 0x80},
                {300000, 0x00, 0x6b}
        };

        int[] selected = null;
        for (int i = 0; i < bauds.length; i++) {
            if (baudRate == bauds[i][0]) {
                selected = bauds[i];
                break;
            }
        }

        if (selected == null) {
            LOG.warn("Could not find baud rate:" + baudRate + ", defaulting to 1200");
            selected = bauds[0];
        }

        // Apply the baud rate
        writeRegister(0x03, selected[2]); // bitrate 7:0
        writeRegister(0x02, selected[1]); // bitrate 15:8

        return selected[0];
    }

    /**
     * Set a filter for demod and AFC depending on the deviation selected
     */
    public void setFilter(int bw) {

        int targetDemod = bw + 400;
        int targetAFC = bw + 2000;

        setDemodFilter(targetDemod);
        setAFCFilter(targetAFC);
    }

    public int setDemodFilter(int targetDemod) {
        // Demod filter
        for (int i = 0; i < filters.length; i++) {
            if (filters[i][0] >= targetDemod) {
                writeRegister(0x12, filters[i][1] + filters[i][2]);
                return filters[i][0];
            }
        }
        return 0;
    }

    public int setAFCFilter(int targetAFC) {
        // AFC fiter
        for (int i = 0; i < filters.length; i++) {
            if (filters[i][0] >= targetAFC) {
                writeRegister(0x13, filters[i][1] + filters[i][2]);
                return filters[i][0];
            }
        }
        return 0;
    }


}