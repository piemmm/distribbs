package org.prowl.distribbs.node.connectivity.gps;

import com.pi4j.io.serial.*;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.HeadingProvider;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.*;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.node.connectivity.sx127x.Modulation;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;

public class GPS extends Interface {

    private static final Log LOG = LogFactory.getLog("GPS");
    private static Position currentPosition;
    private static Double currentCourse;
    private static Double currentHeading;
    private static Double currentSpeed;
    private static Time currentTime;
    private static Date currentDate;
    private HierarchicalConfiguration config;
    private Serial serial;
    private SentenceFactory sf;
    /**
     * Set to true to have threads exit
     */
    private boolean quit;
    private SentenceReader reader;

    public GPS(HierarchicalConfiguration config) {
        this.config = config;
        sf = SentenceFactory.getInstance();
    }

    /**
     * Returns the current known position, or null if no fix
     *
     * @return
     */
    public static Position getCurrentPosition() {
        return currentPosition;
    }

    public static Time getCurrentTime() {
        return currentTime;
    }

    public static Double getCurrentCourse() {
        return currentCourse;
    }

    public static Double getCurrentHeading() {
        return currentHeading;
    }

    public static Double getCurrentSpeed() {
        return currentSpeed;
    }

    public static Date getCurrentDate() {
        return currentDate;
    }

    public void start() throws IOException {


        Tools.runOnThread(() -> {
            setup();
        });

    }

    public void setup() {
        serial = SerialFactory.createInstance();


        // create serial config object
        SerialConfig config = new SerialConfig();

        try {

            config.device(SerialPort.getDefaultPort())
                    .baud(Baud._9600)
                    .dataBits(DataBits._8)
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);

            // open the default serial device/port with the configuration settings
            serial.open(config);

        } catch (Throwable e) {
            // Try 3B+
            try {
                config.device("/dev/ttyS0")
                        .baud(Baud._9600)
                        .dataBits(DataBits._8)
                        .parity(Parity.NONE)
                        .stopBits(StopBits._1)
                        .flowControl(FlowControl.NONE);
                serial.open(config);
            } catch (Throwable ex) {
                LOG.warn("Unable to open serial port, GPS will not be available");
            }

        }


        reader = new SentenceReader(serial.getInputStream());

        HeadingProvider provider = new HeadingProvider(reader);
        provider.addListener(new HeadingListener() {

            @Override
            public void providerUpdate(HeadingEvent evt) {
                currentHeading = evt.getHeading();
            }
        });

        PositionProvider pprovider = new PositionProvider(reader);
        pprovider.addListener(new ProviderListener<PositionEvent>() {

            @Override
            public void providerUpdate(PositionEvent evt) {
                currentPosition = evt.getPosition();
                currentTime = evt.getTime();
                currentCourse = evt.getCourse();
                currentSpeed = evt.getSpeed();
                currentDate = evt.getDate();
            }
        });

        SatelliteInfoProvider sprovider = new SatelliteInfoProvider(reader);
        sprovider.addListener(new SatelliteInfoListener() {

            @Override
            public void providerUpdate(SatelliteInfoEvent evt) {
                GpsFixStatus status = evt.getGpsFixStatus();
                if (status == GpsFixStatus.GPS_NA || status == GpsFixStatus.GPS_2D) {
                    // Pulse GPS led until locked
                    DistriBBS.INSTANCE.getStatus().pulseGPS(150);
                    currentPosition = null;
                } else if (status == GpsFixStatus.GPS_3D) {
                    // LED on all the time
                    DistriBBS.INSTANCE.getStatus().pulseGPS(1500);
                }
            }

        });

        reader.start();


    }

    public void stop() {
        quit = true;
        reader.stop();
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public boolean isAnnounce() {
        return false;
    }

    public int getAnnouncePeriod() {
        return 0;
    }

    public Modulation getModulation() {
        return Modulation.NONE;
    }

    public boolean isRF() {
        return false;
    }

    public boolean canSend() {
        return false;
    }

    public boolean sendPacket(TxRFPacket packet) {
        return false;
    }

    @Override
    public PacketEngine getPacketEngine() {
        return null;
    }

    @Override
    public double getNoiseFloor() {
        return Double.MAX_VALUE;
    }

    public double getRSSI() {
        return 0;
    }

    public int getFrequency() {
        return 0;
    }

    @Override
    public int getSlot() {
        return -1;
    }


    @Override
    public long getTxCompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTxUncompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getRxCompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getRxUncompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }


}