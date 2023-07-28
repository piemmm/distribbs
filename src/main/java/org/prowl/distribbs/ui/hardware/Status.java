package org.prowl.distribbs.ui.hardware;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.lcd.US2066;
import org.prowl.distribbs.leds.StatusLeds;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.utils.EWMAFilter;
import org.prowl.distribbs.utils.Tools;

import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Status {

    private static final Log LOG = LogFactory.getLog("Status");

    private US2066 lcd;
    private StatusLeds leds;
    private Timer tickerTimer;

    private EWMAFilter rssi2m;
    private EWMAFilter rssi70cm;

    private NumberFormat nf;

    public Status() {
        init();
    }

    public void init() {

        rssi2m = new EWMAFilter(0.2f);
        rssi70cm = new EWMAFilter(0.2f);
        nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(1);
        try {
            lcd = new US2066();
            leds = new StatusLeds();
            lcd.writeText(DistriBBS.VERSION_STRING, DistriBBS.INFO_TEXT);

            start();
        } catch (UnsatisfiedLinkError e) {
            // Probably not running on pi
            LOG.warn("Unable to initialise LCD and LEDs, probably not running on a Raspberry Pi");
        }

    }

    public void start() {

        tickerTimer = new Timer();
        tickerTimer.schedule(new TimerTask() {
            private int screen = 0;

            public void run() {

                try {
                    switch (screen % 4) {
                        case 0:
                            screen0();
                            break;
                        case 1:
                            screen1();
                            break;
                        case 2:
                            screen2();
                            break;
                        case 3:
                            screen3();
                            break;
                    }
                } catch (Throwable e) {
                    LOG.debug(e.getMessage(), e);
                }

                screen++;

            }
        }, 2000, 5000);

        // Register our interest in events.
        ServerBus.INSTANCE.register(this);
    }

    public void stop() {
        // Stop listening to events
        ServerBus.INSTANCE.unregister(this);
    }

    public void screen0() {

        String topString = "Idle";
        String bottomString = "No new messages";

        setText(topString.toString(), bottomString.toString());
    }

    public void screen1() {

        String topString = "MHeard:";
        String bottomString = " none";

        List<Node> heardList = DistriBBS.INSTANCE.getStatistics().getHeard().listHeard();
        StringBuilder b = new StringBuilder();
        if (heardList.size() > 0) {
            bottomString = "";
            for (Node n : heardList) {
                String callsign = n.getCallsign();
                if (topString.length() < 20 - (callsign.length())) {
                    topString += callsign + " ";
                } else {
                    bottomString += callsign + " ";
                }

            }
        }

        setText(topString.toString(), bottomString.toString());
    }

    public void screen2() {
        String topString = "Status: OK";
        String bottomString = "IP: -";
        bottomString = Tools.getDefaultOutboundIP().getHostAddress();

        setText(topString.toString(), bottomString.toString());
    }

    public void screen3() {
        long finish = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < finish) {
            List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getPorts();
            String topString = "2m: Not configured";
            String bottomString = "70cm: Not configured";
            for (Interface c : connectors) {
                int freq = c.getFrequency();
                if (freq > 143000000 && freq < 147000000) {
                    float current = rssi2m.addPoint((float) c.getRSSI());
                    topString = "2m RSSI: -" + (nf.format(current) + " dBm");
                } else if (freq > 430000000 && freq < 450000000) {
                    float current = rssi70cm.addPoint((float) c.getRSSI());
                    bottomString = "70cm RSSI: -" + (nf.format(current) + " dBm");

                }
            }
            setText(topString, bottomString);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

    }

    public void setText(String line1, String line2) {
        lcd.writeText(line1, line2);
    }

    public void pulseGPS(long time) {
        leds.pulseGPS(time);
    }

    public void setMessageBlink(boolean shouldBlink, long blinkRate) {
        leds.setMessageBlink(shouldBlink, blinkRate);
    }

    public void setLink(boolean on) {
        leds.setLink(on);
    }

    @Subscribe
    public void pulseTRX(RxRFPacket packet) {
        //   leds.pulseTRX(50);
    }

    @Subscribe
    public void pulseTRX(TxRFPacket packet) {
        leds.pulseTRX(50);
    }

}
