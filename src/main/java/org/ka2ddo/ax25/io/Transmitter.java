//package org.ka2ddo.ax25.io;
///*
// * Copyright (C) 2011-2023 Andrew Pavlin, KA2DDO
// * This file is part of YAAC (Yet Another APRS Client).
// *
// *  YAAC is free software: you can redistribute it and/or modify
// *  it under the terms of the GNU Lesser General Public License as published by
// *  the Free Software Foundation, either version 3 of the License, or
// *  (at your option) any later version.
// *
// *  YAAC is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *  GNU General Public License for more details.
// *
// *  You should have received a copy of the GNU General Public License
// *  and GNU Lesser General Public License along with YAAC.  If not,
// *  see <http://www.gnu.org/licenses/>.
// */
//
//import org.ka2ddo.aprs.APRSStack;
//import org.ka2ddo.aprs.AprsSignableMessage;
//import org.ka2ddo.ax25.AX25Callsign;
//import org.ka2ddo.ax25.AX25Frame;
//import org.ka2ddo.ax25.AX25FrameSource;
//import org.ka2ddo.ax25.AX25Message;
//import org.ka2ddo.ax25.AX25Stack;
//import org.ka2ddo.ax25.ConnState;
//import org.ka2ddo.ax25.ConnectionEstablishmentListener;
//import org.ka2ddo.ax25.Connector;
//import org.ka2ddo.ax25.DigipeatAliasCatalog;
//import org.ka2ddo.ax25.DigipeatAliasRecord;
//import org.ka2ddo.ax25.ProtocolFamily;
//import org.ka2ddo.ax25.SendableMessage;
//import org.ka2ddo.ax25.SignableMessage;
//import org.ka2ddo.ax25.Transmitting;
//import org.ka2ddo.ax25.TransmittingConnector;
//import org.ka2ddo.opentrac.OpenTracEntity;
//import org.ka2ddo.opentrac.OpenTracMessage;
//import org.ka2ddo.opentrac.OpenTracPathTrace;
//import org.ka2ddo.opentrac.OpenTracTypes;
//import org.ka2ddo.opentrac.TraceStep;
//import org.ka2ddo.util.DebugCtl;
//import org.ka2ddo.yaac.auth.YAACKeyManager;
//import org.ka2ddo.yaac.YAAC;
//import org.ka2ddo.yaac.ax25.Digipeater;
//import org.ka2ddo.yaac.core.AX25Logger;
//import org.ka2ddo.yaac.core.PacketExportMode;
//import org.ka2ddo.aprs.SetBeaconRatesIfc;
//import org.ka2ddo.yaac.core.YAACPreferences;
//
//import java.io.Closeable;
//import java.net.UnknownServiceException;
//import java.security.GeneralSecurityException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.prefs.Preferences;
//
///**
// * This class handles scheduling transmit requests to the various ports.
// * It handles retransmissions with backoffs, and reschedulings upon data
// * updates, and caching transmit-related parameters.
// * @author Andrew Pavlin, KA2DDO
// */
//public final class Transmitter extends PortManager implements Runnable, Transmitting, SetBeaconRatesIfc, Closeable {
//
//    private static final ProtocolFamily[] PROTOCOL_FAMILIES = ProtocolFamily.values();
//
//    /**
//     * Wrapper class for frames delay-queued for transmission.
//     */
//    private static class TimedQueueEntry implements Comparable<TimedQueueEntry> {
//        TimedQueueEntry next = null;
//        AX25FrameSource frameSource;
//        long dueTime;
//
//        TimedQueueEntry(AX25FrameSource frameSource, long dueTime) {
//            this.frameSource = frameSource;
//            this.dueTime = dueTime;
//        }
//
//        /**
//         * Compares this object with the specified object for order.  Returns a
//         * negative integer, zero, or a positive integer as this object is less
//         * than, equal to, or greater than the specified object.
//         *
//         * @param o the object to be compared.
//         * @return a negative integer, zero, or a positive integer as this object
//         *         is less than, equal to, or greater than the specified object.
//         * @throws NullPointerException if the specified object is null
//         * @throws ClassCastException   if the specified object's type prevents it
//         *                              from being compared to this object.
//         */
//        public int compareTo(TimedQueueEntry o) {
//            return Long.signum(dueTime - o.dueTime);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj instanceof TimedQueueEntry) {
//                TimedQueueEntry tqe = (TimedQueueEntry)obj;
//                if (dueTime == tqe.dueTime) {
//                    return frameSource.equals(tqe.frameSource);
//                }
//            }
//            return false;
//        }
//
//        @Override
//        public int hashCode() {
//            return (int)dueTime;
//        }
//
//        @Override
//        public String toString() {
//            return "TimedQueueEntry[@" + dueTime + ',' + frameSource + ']';
//        }
//    }
//
//    /**
//     * Default choices for specifying a digipeat path.
//     */
//    private static final String[] STANDARD_DIGIPEAT_CHOICES = { " ", "WIDE1-1", "WIDE1-1,WIDE2-1", "TEMP1-1", "TEMP2-2" };
//    /**
//     * Suggested path for messaging through the International Space Station.
//     */
//    public static final String ARISS_DIGIPEAT_CHOICE = "ARISS,SGATE,WIDE2-1";
//    /**
//     * Suggested path to forward to the Outernet.
//     */
//    public static final String OUTNET_CHOICE = "OUTNET";
//    private final ArrayList<AX25FrameSource> queue = new ArrayList<>();
//    private transient TimedQueueEntry delayQueueHead = null;
//    private final AX25Logger logger = new AX25Logger("AX25xmit");
//    private int decayRatio;
//    private int initialSendRate;
//    private int slowSendRate;
//    private int slowSpeed;
//    private int fastSpeed;
//    private int minTurnAngle;
//    private int turnSlope;
//    private int retransmitCount;
//    private String defaultDigipeatPath;
//    private static final String XMT_SLOW_SPEED = "XmtSlowSpeed";
//    private static final String XMT_FAST_SPEED = "XmtFastSpeed";
//    private static final String XMT_SLOW_SEND_RATE = "XmtSlowSendRate";
//    private static final String XMT_INITIAL_SEND_RATE = "XmtInitialSendRate";
//    private static final String XMT_DECAY_RATIO = "XmtBackoffRatio";
//    private static final String XMT_MIN_TURN_ANGLE = "XmtMinTurnAngle";
//    private static final String XMT_TURN_SLOPE = "XmtTurnSlope";
//    private static final String XMT_RETRANS_COUNT = "XmtRetransmitCount";
//    private static final Transmitter instance = new Transmitter();
//
//    private Transmitter() {
//        Preferences prefs = YAAC.getPreferences();
//        decayRatio = prefs.getInt(XMT_DECAY_RATIO, 2);
//        initialSendRate = prefs.getInt(XMT_INITIAL_SEND_RATE, 60);
//        slowSendRate = prefs.getInt(XMT_SLOW_SEND_RATE, 1800);
//        slowSpeed = prefs.getInt(XMT_SLOW_SPEED, 5);
//        fastSpeed = prefs.getInt(XMT_FAST_SPEED, 70);
//        minTurnAngle = prefs.getInt(XMT_MIN_TURN_ANGLE, 10);
//        turnSlope = prefs.getInt(XMT_TURN_SLOPE, 240);
//        retransmitCount = prefs.getInt(XMT_RETRANS_COUNT, 3);
//        defaultDigipeatPath = prefs.get(YAACPreferences.DEFAULT_DIGIPEAT_PATH, "WIDE1-1,WIDE2-1");
//        YAAC.addShutdownHandler(logger);
//        if (DebugCtl.isDebug("thread")) {
//            new Throwable("starting Transmitter thread...").printStackTrace(System.out);
//        }
//        Thread t = new Thread(this, "Transmitter Thread");
//        t.setDaemon(true);
//        t.start();
//    }
//
//    /**
//     * Get a reference to the singleton Transmitter object.
//     * @return the Transmitter object
//     */
//    public static Transmitter getInstance() {
//        return instance;
//    }
//
//    /**
//     * Build a sorted list of all the currently registered known choices for
//     * digipeat alias paths.
//     * @return array of alias Strings
//     */
//    public static String[] getStandardDigipeatPathChoices() {
//        ArrayList<String> choiceList = new ArrayList<>();
//        for (int i = 0; i < STANDARD_DIGIPEAT_CHOICES.length; i++) {
//            choiceList.add(STANDARD_DIGIPEAT_CHOICES[i]);
//        }
//        for (DigipeatAliasRecord dar : DigipeatAliasCatalog.getInstance()) {
//            String darString = dar.getAliasString();
//            if (dar.enabled && !choiceList.contains(darString)) {
//                choiceList.add(darString);
//            }
//        }
//        Collections.sort(choiceList);
//        choiceList.add(ARISS_DIGIPEAT_CHOICE);
//        choiceList.add(OUTNET_CHOICE);
//        return choiceList.toArray(new String[choiceList.size()]);
//    }
//
//    /**
//     * Return the longest range digipeat alias path configured for the local station.
//     * @return digipeat path string
//     */
//    public static String getFarthestReachingDigipeatPathChoice() {
//        ArrayList<String> choiceList = new ArrayList<>();
//        for (int i = 0; i < STANDARD_DIGIPEAT_CHOICES.length; i++) {
//            choiceList.add(STANDARD_DIGIPEAT_CHOICES[i]);
//        }
//        for (DigipeatAliasRecord dar : DigipeatAliasCatalog.getInstance()) {
//            String darString = dar.getAliasString();
//            if (dar.enabled && !choiceList.contains(darString)) {
//                choiceList.add(darString);
//            }
//        }
//        String farthestChoice = null;
//        int farthestRange = -1;
//        for (String choice : choiceList) {
//            int range = 1; // at least have direct transmission
//            String[] digis = AX25Message.split(choice, ',');
//            for (String digi : digis) {
//                if ("RFONLY".equals(digi) || "NOGATE".equals(digi)) {
//                    continue;
//                }
//                int hyphenPos = digi.indexOf('-');
//                if (hyphenPos > 1 && hyphenPos < digi.length() - 1) {
//                    char tailBase = digi.charAt(hyphenPos - 1);
//                    if (tailBase >= '0' && tailBase <= '7') {
//                        // New-N paradigm alias, how much is left
//                        int stepRange = Integer.parseInt(digi.substring(hyphenPos + 1), 10);
//                        range += stepRange;
//                    }
//                } else if (-1 == hyphenPos) {
//                    char tailBase = digi.charAt(digi.length() - 1);
//                    if (tailBase < '0' || tailBase > '7') {
//                        // not a New-N paradigm alias that's been used up
//                        range++;
//                    }
//                }
//            }
//            if (range > farthestRange) {
//                farthestRange = range;
//                farthestChoice = choice;
//            }
//        }
//        return farthestChoice;
//    }
//
//    /**
//     * Get the scaling factor by which the message repeat interval is enlarged
//     * (until the default slow send rate is reached).
//     * @return int decay ratio
//     */
//    public int getDecayRatio() {
//        return decayRatio;
//    }
//
//    /**
//     * Set the scaling factor by which the message repeat interval is enlarged
//     * (until the default slow send rate is reached).
//     * @param decayRatio int decay ratio (should be 2 or 3)
//     */
//    public void setDecayRatio(int decayRatio) {
//        this.decayRatio = decayRatio;
//        YAAC.getPreferences().putInt(XMT_DECAY_RATIO, decayRatio);
//    }
//
//    /**
//     * Get the time interval between retransmissions when a message is newly
//     * introduced into the system.
//     * @return send interval in seconds
//     */
//    public int getInitialSendRate() {
//        return initialSendRate;
//    }
//
//    /**
//     * Set the time interval between retransmissions when a message is newly
//     * introduced into the system.
//     * @param initialSendRate send interval in seconds
//     */
//    public void setInitialSendRate(int initialSendRate) {
//        this.initialSendRate = initialSendRate;
//        YAAC.getPreferences().putInt(XMT_INITIAL_SEND_RATE, initialSendRate);
//    }
//
//    /**
//     * The slowest rate a message should be sent. For messages to be terminated, they
//     * should stop being requeued when the retransmission interval is enlarged to this.
//     * @return transmission interval in seconds
//     */
//    public int getSlowSendRate() {
//        return slowSendRate;
//    }
//
//    /**
//     * Set the slowest rate a message should be sent. For messages to be terminated, they
//     * should stop being requeued when the retransmission interval is enlarged to this.
//     * @param slowSendRate transmission interval in seconds
//     */
//    public void setSlowSendRate(int slowSendRate) {
//        this.slowSendRate = slowSendRate;
//        YAAC.getPreferences().putInt(XMT_SLOW_SEND_RATE, slowSendRate);
//    }
//
//    /**
//     * Indicate whether beacon data source can dynamically change its position (latitude/longitude),
//     * such as for a mobile station with a GPS.
//     *
//     * @return boolean true if latitude or longitude of beacon can change dynamically (not just through UI)
//     */
//    public boolean isUseGpsForPosition() {
//        return false;
//    }
//
//    /**
//     * Slowest speed of station motion such that position message transmissions are scheduled
//     * as if station was not moving.
//     * @return slowest speed in current speed units (nm, mi, or km)
//     * @see org.ka2ddo.util.DistanceUnit
//     */
//    public int getSlowSpeed() {
//        return slowSpeed;
//    }
//
//    /**
//     * Set slowest speed of station motion such that position message transmissions are scheduled
//     * as if station was not moving.
//     * @param slowSpeed slowest speed in current speed units (nm, mi, or km)
//     * @see org.ka2ddo.util.DistanceUnit
//     */
//    public void setSlowSpeed(int slowSpeed) {
//        this.slowSpeed = slowSpeed;
//        YAAC.getPreferences().putInt(XMT_SLOW_SPEED, slowSpeed);
//    }
//
//    /**
//     * Fastest speed of station motion such that position message transmission intervals
//     * are maximized to the initial transmission rate, regardless of how fast the station
//     * is actually moving.
//     * @return fastest speed in current speed units (nm/hr, mi/hr, or km/hr)
//     * @see org.ka2ddo.util.DistanceUnit
//     */
//    public int getFastSpeed() {
//        return fastSpeed;
//    }
//
//    /**
//     * Set fastest speed of station motion such that position message transmission intervals
//     * are maximized to the initial transmission rate, regardless of how fast the station
//     * is actually moving.
//     * @param fastSpeed fastest speed in current speed units (nm/hr, mi/hr, or km/hr)
//     * @see org.ka2ddo.util.DistanceUnit
//     */
//    public void setFastSpeed(int fastSpeed) {
//        this.fastSpeed = fastSpeed;
//        YAAC.getPreferences().putInt(XMT_FAST_SPEED, fastSpeed);
//    }
//
//    /**
//     * Get the minimum amount of turn angle change before YAAC will accelerate reporting beacon position
//     * updates ("corner pegging"). This will be adjusted by the speed-specific turn slope.
//     * @return the current minimum turn angle
//     * @see #getTurnSlope()
//     */
//    public int getMinTurnAngle() {
//        return minTurnAngle;
//    }
//
//    /**
//     * Set the minimum amount of turn angle change before YAAC will accelerate reporting beacon position
//     * updates ("corner pegging"). This will be adjusted by the speed-specific turn slope.
//     * @param minTurnAngle the current minimum turn angle
//     */
//    public void setMinTurnAngle(int minTurnAngle) {
//        if (minTurnAngle < 1) {
//            throw new IllegalArgumentException("minTurnAngle must be positive");
//        }
//        this.minTurnAngle = minTurnAngle;
//        YAAC.getPreferences().putInt(XMT_MIN_TURN_ANGLE, minTurnAngle);
//    }
//
//    /**
//     * Get the turn slope, which is the scaling factor for the increase over the minimum turn angle before
//     * beacon retransmissions will be accelerated. The actual amount of turn needed to cause a beacon
//     * retransmission is getMinTurnAngle() + GPSDistributor.getInstance().getCurrentFix().speed / getTurnSlope()
//     * @return the turn slope scaling factor
//     * @see #getMinTurnAngle()
//     */
//    public int getTurnSlope() {
//        return turnSlope;
//    }
//
//    /**
//     * Set the turn slope, which is the scaling factor for the increase over the minimum turn angle before
//     * beacon retransmissions will be accelerated. The actual amount of turn needed to cause a beacon
//     * retransmission is getMinTurnAngle() + GPSDistributor.getInstance().getCurrentFix().speed / getTurnSlope()
//     * @param turnSlope the turn slope scaling factor
//     */
//    public void setTurnSlope(int turnSlope) {
//        if (turnSlope <= 0) {
//            throw new IllegalArgumentException("turn slope must be positive");
//        }
//        this.turnSlope = turnSlope;
//        YAAC.getPreferences().putInt(XMT_TURN_SLOPE, turnSlope);
//    }
//
//    /**
//     * Get the locally-originated message retransmit count.
//     * @return retransmit count
//     */
//    public int getRetransmitCount() {
//        return retransmitCount;
//    }
//
//    /**
//     * Set the locally-originated message retransmit count.
//     * @param retransmitCount number of times to retransmit a locally transmitted message when not acknowledged
//     */
//    public void setRetransmitCount(int retransmitCount) {
//        if (this.retransmitCount != retransmitCount) {
//            this.retransmitCount = retransmitCount;
//            YAAC.getPreferences().putInt(XMT_RETRANS_COUNT, retransmitCount);
//        }
//    }
//
//    /**
//     * Get the default sequence of digipeat aliases that should be used for locally
//     * originated messages (assuming proportional pathing isn't being used).
//     * @return comma-separated ordered list of digipeat aliases
//     */
//    public String getDefaultDigipeatPath() {
//        return defaultDigipeatPath;
//    }
//
//    /**
//     * Set the default sequence of digipeat aliases that should be used for locally
//     * originated messages (assuming proportional pathing isn't being used).
//     * @param defaultDigipeatPath comma-separated ordered list of digipeat aliases
//     */
//    public void setDefaultDigipeatPath(String defaultDigipeatPath) {
//        this.defaultDigipeatPath = defaultDigipeatPath;
//        YAAC.getPreferences().put(YAACPreferences.DEFAULT_DIGIPEAT_PATH, defaultDigipeatPath);
//    }
//
//    /**
//     * Get the current format for AX.25 message logging.
//     * @return PacketExportMode in use
//     */
//    public PacketExportMode getLoggerFormat() {
//        return logger.getLogFileFormat();
//    }
//
//    /**
//     * Set the format for AX.25 message logging.
//     * @param pem PacketExportMode to use
//     */
//    public void setLoggerFormat(PacketExportMode pem) {
//        logger.setLogFileFormat(pem);
//    }
//
//    /**
//     * Queue the specified frame source for transmission over the specified (or all, if not
//     * specified) transmit-enabled PortConnectors.
//     * @param entry AX25FrameSource of the frame to be transmitted
//     */
//    public synchronized void queue(AX25FrameSource entry) {
//        queue.add(entry);
//        notifyAll();
//    }
//
//    /**
//     * Queue the specified frame source for transmission over the specified (or all, if not
//     * specified) transmit-enabled PortConnectors.
//     * @param entry AX25FrameSource of the frame to be transmitted
//     * @param timeToSend long time in milliseconds since Unix epoch when packet is to be dequeued and transmitted
//     */
//    public synchronized void delayedQueue(AX25FrameSource entry, long timeToSend) {
//        if (timeToSend <= System.currentTimeMillis()) {
//            queue.add(entry); // overdue, send now
//        } else {
//            TimedQueueEntry tqe = new TimedQueueEntry(entry, timeToSend);
//            if (delayQueueHead == null) {
//                delayQueueHead = tqe;
//            } else if (tqe.compareTo(delayQueueHead) < 0) {
//                tqe.next = delayQueueHead;
//                delayQueueHead = tqe;
//            } else {
//                TimedQueueEntry prev = delayQueueHead;
//                while (prev.next != null && prev.next.compareTo(tqe) < 0) {
//                    prev = prev.next;
//                }
//                tqe.next = prev.next;
//                prev.next = tqe;
//            }
//        }
//        notifyAll();
//    }
//
//    /**
//     * DO NOT CALL. Internal transmit dispatch queue reader.
//     */
//    public void run() {
//        AX25FrameSource entry = null;
//        try {
//            while (true) {
//                try {
//                    entry = null;
//                    synchronized (this) {
//                        if (delayQueueHead == null) {
//                            while (queue.size() == 0) {
//                                try {
//                                    wait();
//                                } catch (InterruptedException e) {
//                                    // do nothing, we expect to be interrupted
//                                }
//                            }
//                        } else {
//                            long now = System.currentTimeMillis();
//                            do {
//                                if (now < delayQueueHead.dueTime) {
//                                    try {
//                                        wait(delayQueueHead.dueTime - now);
//                                    } catch (InterruptedException e) {
//                                        // do nothing, we expect to be interrupted
//                                    }
//                                    now = System.currentTimeMillis();
//                                }
//                            } while (queue.size() == 0 && delayQueueHead != null && now < delayQueueHead.dueTime);
//                        }
//                        if (queue.size() > 0) {
//                            entry = queue.remove(0);
//                        } else if (delayQueueHead != null) {
//                            if (delayQueueHead.dueTime <= System.currentTimeMillis()) {
//                                entry = delayQueueHead.frameSource;
//                                delayQueueHead = delayQueueHead.next;
//                            }
//                        }
//                    }
//                    if (entry != null) {
//                        long now = System.currentTimeMillis();
//                        PortConnector p = (PortConnector)entry.getConnector();
//                        boolean incrementXmitCount = true;
//                        BeaconData beaconData = null;
//                        if (entry instanceof BeaconData) {
//                            beaconData = (BeaconData)entry;
//                        }
//                        if (p == null) {
//                            for (PortConnector port : portList) {
//                                if (port.isOpen() && port.hasCapability(Connector.CAP_XMT_PACKET_DATA) &&
//                                        (beaconData == null || beaconData.isMatchingBeacon(port.currentCfg.beaconNames))) {
//                                    incrementXmitCount = sendForOnePort(entry, now, (TransmittingConnector)port, incrementXmitCount);
//                                }
//                            }
//                        } else if (p.hasCapability(Connector.CAP_XMT_PACKET_DATA)) {
//                            incrementXmitCount = sendForOnePort(entry, now, (TransmittingConnector)p, incrementXmitCount);
//                        }
//                    }
//                } catch (ThreadDeath td) {
//                    throw td;
//                } catch (Throwable e) {
//                    if (entry != null) {
//                        System.out.println("*** unhandled exception in TransmitterThread with entry type " + entry.getClass().getSimpleName() + ": " + entry);
//                    } else {
//                        System.out.println("*** unhandled exception in TransmitterThread with null entry");
//                    }
//                    e.printStackTrace(System.out);
//                }
//            }
//        } finally {
//            System.out.println(new Date().toString() + ": terminating TransmitterThread");
//        }
//    }
//
//    /**
//     * Transmit the specified frame out one port.
//     * @param entry the AX25FrameSource implementor that will provide the frames to transmit
//     * @param now current time in Unix milliseconds since 1970 UTC
//     * @param p the port to transmit through
//     * @param incrementXmitCount whether or not this is a new round of transmissions, such that proportional pathing should advance to the next path combination
//     * @return boolean true if transmit didn't happen for some reason, such that any next call on the same frame should pass the increment flag again
//     */
//    private static boolean sendForOnePort(AX25FrameSource entry, long now, TransmittingConnector p, boolean incrementXmitCount) {
//        AX25Frame[] frames;
//        // try all supported protocols if message didn't have a default
//        int acceptableProtocolsMask = p.getAcceptableProtocolsMask();
//        List<AX25Frame[]> alreadySentFrames = new ArrayList<>();
//        proto_loop:
//        for (int i = 0; i < PROTOCOL_FAMILIES.length; i++) {
//            if ((acceptableProtocolsMask & (1 << i)) != 0) {
//                frames = entry.getFrames(incrementXmitCount, PROTOCOL_FAMILIES[i], p.getCallsign());
//                if (frames != null && frames.length > 0) {
//                    for (AX25Frame[] oldSet : alreadySentFrames) {
//                        if (Arrays.equals(oldSet, frames)) {
//                            continue proto_loop;
//                        }
//                    }
//                    alreadySentFrames.add(frames);
//                    for (AX25Frame frame : frames) {
//                        if (frame != null) {
//                            if (frame.rcptTime < 0L) {
//                                frame.rcptTime = now;
//                            }
//                            boolean sentSuccessfully = sendOneFrame(entry, now, frame, p);
//                            incrementXmitCount &= !sentSuccessfully;
//                        } else {
//                            System.out.println(new Date().toString() + ": received null frame from " + entry);
//                        }
//                    }
//                }
//            }
//        }
//        return incrementXmitCount;
//    }
//
//    private static boolean sendOneFrame(AX25FrameSource entry, long now, AX25Frame frame, TransmittingConnector p) {
//        if ((frame = sendToPort(frame, p, now, p.getCapabilities())) != null) {
//            Digipeater.getInstance().rememberLocallyOriginatedMessage(frame);
//            frame.rcptTime = now;
//            frame.sourcePort = (Connector)p; // ensure logger knows which port this was transmitted through
//            if (entry instanceof SendableMessageWrapper) {
//                // locally originated APRS message, so record that it was sent.
//                // (we shouldn't digipeat the message again because we just logged
//                //   that we already transmitted it)
//                if ((frame.ctl & ~AX25Frame.MASK_U_P) == (AX25Frame.FRAMETYPE_U|AX25Frame.UTYPE_UI) && AX25Frame.PID_NOLVL3 == frame.pid) {
//                    APRSStack.getInstance().processParsedAX25Packet(frame, frame.parsedAX25Msg);
//                } else {
//                    AX25Stack.getInstance().processParsedAX25Message(frame, frame.parsedAX25Msg);
//                }
//            }
//            return true;
//        }
//        return false;
//    }
//
//    private static AX25Frame sendToPort(AX25Frame frame, TransmittingConnector port, long now, int capabilities) {
//        try {
//            boolean isLocalOrigin = false;
//            if (!port.isOpen() || (capabilities & Connector.CAP_XMT_PACKET_DATA) == 0) {
//                return null; // can't transmit, so don't waste time doing further analysis
//            } else if ((capabilities & Connector.CAP_RF) != 0) {
//                PortConfig.Cfg cfg = ((PortConnector)port).currentCfg;
//                if (frame.sender == null || (frame.sourcePort != null && frame.sourcePort.hasCapability(Connector.CAP_IGATE))) {
//                    AX25Frame dupFrame = frame.dup();
//                    if (frame.sender == null) {
//                        isLocalOrigin = true;
//                    }
//                    dupFrame.sender = new AX25Callsign(cfg.callsign);
//                    dupFrame.sender.h_c = !frame.dest.h_c;
//                    if (frame.parsedAX25Msg != null) {
//                        AX25Message dupAprsMsg = frame.parsedAX25Msg.dup();
//                        dupAprsMsg.setAx25Frame(dupFrame);
//                        dupFrame.parsedAX25Msg = dupAprsMsg;
//                        if (dupAprsMsg.getTimestamp() == dupAprsMsg.getRcptTime()) {
//                            // only "fix" the message timestamp if it doesn't have embedded time in the text
//                            dupAprsMsg.setTimestamp(now);
//                        }
//                        dupAprsMsg.setRcptTime(now);
//                        if (dupAprsMsg.originatingCallsign == null) {
//                            dupAprsMsg.originatingCallsign = dupFrame.sender.toString();
//                        }
//                    }
//                    frame = dupFrame;
//                }
//                if (frame.getFrameType() == AX25Frame.FRAMETYPE_U && frame.getUType() == AX25Frame.UTYPE_UI) {
//                    switch (frame.pid) {
//                        case AX25Frame.PID_NOLVL3:
//                            if (frame.parsedAX25Msg != null && frame.parsedAX25Msg instanceof AprsSignableMessage) {
//                                AprsSignableMessage asm = (AprsSignableMessage)frame.parsedAX25Msg;
//                                YAACKeyManager yaacKeyManager = YAACKeyManager.getInstance();
//                                if (asm.getSignatureState() == SignableMessage.SignatureState.NEEDS_SIGNATURE &&
//                                        yaacKeyManager.isPasswordSet()) {
//                                    String keyAlias = asm.getKeyAlias();
//                                    try {
//                                        yaacKeyManager.sign(asm, keyAlias);
//                                        frame.body = ((SendableMessage)frame.parsedAX25Msg).getBody(false, ProtocolFamily.APRS, frame);
//                                    } catch (GeneralSecurityException e) {
//                                        e.printStackTrace(System.out);
//                                    }
//                                }
//                            }
//                            break;
//                        case AX25Frame.PID_OPENTRAC:
//                            if (!(frame.parsedAX25Msg instanceof OpenTracMessage)) {
//                                return null; // original message wasn't OpenTRAC
//                            }
//                            // if PathTrace exists, add this station to the trace
//                            OpenTracMessage otm = (OpenTracMessage)frame.parsedAX25Msg;
//                            //TODO: figure out if we should add a PathTrace because we are tracing on a digipeat alias
//                            int hyphenPos = cfg.callsign.indexOf('-');
//                            int ssid = 0;
//                            String baseCallsign;
//                            if (hyphenPos > 0) {
//                                baseCallsign = cfg.callsign.substring(0, hyphenPos);
//                                ssid = Integer.parseInt(cfg.callsign.substring(hyphenPos + 1).trim());
//                            } else {
//                                baseCallsign = cfg.callsign;
//                            }
//                            boolean modified = false;
//                            for (int i = 0; i < otm.entities.size(); i++) {
//                                OpenTracEntity ote = otm.entities.get(i);
//                                if (!ote.isEntityAddressed()) {
//                                    ote.setCallsign(cfg.callsign);
//                                    modified = true;
//                                }
//                                OpenTracPathTrace otpt = (OpenTracPathTrace)ote.getMatchingElement(OpenTracTypes.OPENTRAC_PATH_TRACE);
//                                if (otpt != null) {
//                                    TraceStep ts = new TraceStep(baseCallsign, ssid,
//                                            (cfg.flags & PortConfig.FLAGS_HF) != 0 ? OpenTracTypes.NETWORKTYPE_HF : OpenTracTypes.NETWORKTYPE_VHF);
//                                    TraceStep[] steps = new TraceStep[otpt.steps.length + 1];
//                                    System.arraycopy(otpt.steps, 0, steps, 0, otpt.steps.length);
//                                    steps[otpt.steps.length] = ts;
//                                    otpt.steps = steps;
//                                    modified = true;
//                                }
//                            }
//                            // now that OpenTRAC message has path trace updated, regenerate frame from it
//                            if (modified) {
//                                frame.body = otm.getBody(true, ProtocolFamily.OPENTRAC, frame);
//                            }
//                            //TODO: how to sign OpenTRAC message?
//                            break;
//                        default:
//                            break;
//                    }
//                } else { // some other frametype than UI
//                    if ((cfg.acceptableProtocolsMask & PortConfig.PROTOCOL_AX25) == 0) {
//                        return null; // not authorized for this protocol
//                    }
//                }
//                if (DebugCtl.isDebug()) System.out.println(new Date(now).toString() + " sending " + frame + " to port " + port.toString());
//                if (isLocalOrigin) {
//                    Connector.PortStats localStats = ((PortConnector) port).getStats(PortConnector.PortStatsType.LOCAL_ORIGIN);
//                    localStats.numXmtFrames++;
//                    localStats.numXmtBytes += frame.getEstimatedBitCount()/8;
//                }
//                port.sendFrame(frame);
//                return frame;
//            } else if ((capabilities & (Connector.CAP_IGATE|Connector.CAP_OPENTRAC)) == Connector.CAP_IGATE) {
//                if (frame.getFrameType() == AX25Frame.FRAMETYPE_U && frame.getUType() == AX25Frame.UTYPE_UI) {
//                    switch (frame.pid) {
//                        case AX25Frame.PID_NOLVL3:
//                            boolean nogate = false;
//                            if (frame.digipeaters != null && frame.digipeaters.length > 0) {
//                                for (int i = 0; i < frame.digipeaters.length; i++) {
//                                    String baseCallsign = frame.digipeaters[i].getBaseCallsign();
//                                    if (baseCallsign.equalsIgnoreCase("RFONLY") ||
//                                            baseCallsign.equalsIgnoreCase("NOGATE")) {
//                                        nogate = true;
//                                        break;
//                                    }
//                                }
//                            }
//                            if (!nogate) {
//                                PortConfig.Cfg cfg = ((PortConnector)port).currentCfg;
//                                if (frame.sender == null || (frame.sourcePort != null && frame.sourcePort.hasCapability(Connector.CAP_IGATE))) {
//                                    AX25Frame dupFrame = frame.dup();
//                                    if (frame.sender== null) {
//                                        isLocalOrigin = true;
//                                    }
//                                    dupFrame.sender = new AX25Callsign(cfg.callsign);
//                                    dupFrame.sender.h_c = !frame.dest.h_c;
//                                    if (frame.parsedAX25Msg != null) {
//                                        AX25Message dupAprsMsg = frame.parsedAX25Msg.dup();
//                                        dupAprsMsg.setAx25Frame(dupFrame);
//                                        dupFrame.parsedAX25Msg = dupAprsMsg;
//                                        if (dupAprsMsg.getTimestamp() == dupAprsMsg.getRcptTime()) {
//                                            // only "fix" the message timestamp if it doesn't have embedded time in the text
//                                            dupAprsMsg.setTimestamp(now);
//                                        }
//                                        dupAprsMsg.setRcptTime(now);
//                                        if (dupAprsMsg.originatingCallsign == null) {
//                                            dupAprsMsg.originatingCallsign = dupFrame.sender.toString();
//                                        }
//                                    }
//                                    frame = dupFrame;
//                                }
//                                if (frame.parsedAX25Msg != null && frame.parsedAX25Msg instanceof AprsSignableMessage) {
//                                    AprsSignableMessage asm = (AprsSignableMessage)frame.parsedAX25Msg;
//                                    YAACKeyManager yaacKeyManager = YAACKeyManager.getInstance();
//                                    if (asm.getSignatureState() == SignableMessage.SignatureState.NEEDS_SIGNATURE &&
//                                            yaacKeyManager.isPasswordSet()) {
//                                        String keyAlias = asm.getKeyAlias();
//                                        try {
//                                            yaacKeyManager.sign(asm, keyAlias);
//                                            frame.body = ((SendableMessage)frame.parsedAX25Msg).getBody(false, ProtocolFamily.APRS, frame);
//                                        } catch (GeneralSecurityException e) {
//                                            e.printStackTrace(System.out);
//                                        }
//                                    }
//                                }
//                                if (DebugCtl.isDebug())
//                                    System.out.println(new Date(now).toString() + " sending " + frame + " to port " + port.toString());
//                                if (isLocalOrigin) {
//                                    Connector.PortStats localStats = ((PortConnector) port).getStats(PortConnector.PortStatsType.LOCAL_ORIGIN);
//                                    localStats.numXmtFrames++;
//                                    localStats.numXmtBytes += frame.getEstimatedBitCount()/8;
//                                }
//                                port.sendFrame(frame);
//                                return frame;
//                            }
//                            break;
//                        case AX25Frame.PID_OPENTRAC:
//                            //TODO: convert OpenTrac message to APRS
//                    }
//                }
//                // APRS-IS ports only accept APRS packets, no other protocol
//            }
//            // other port types can't transmit AX.25 frames
//        } catch (IOException e) {
//            port.getStats().numBadXmtFrames++;
//            System.out.print(new Date(now).toString() + " failure to transmit to " + port + ": ");
//            e.printStackTrace(System.out);
//        }
//        return null;
//    }
//
//    /**
//     * Test if this callsign is addressed to the local station.
//     * @param destCallsign String of AX.25 callsign-SSID to test as a destination
//     * @return boolean true if this callsign is for the local station
//     */
//    public boolean isLocalDest(String destCallsign) {
//        if (destCallsign != null && destCallsign.length() > 0) {
//            ArrayList<? extends Connector> portList = PortManager.getPortList();
//            for (int i = portList.size() - 1; i >= 0; i--) {
//                Connector conn;
//                if ((conn = portList.get(i)) != null && conn.isOpen() && conn.hasCapability(Connector.CAP_RCV_PACKET_DATA)) {
//                    String connCallsign;
//                    if ((connCallsign = conn.getCallsign()) != null && destCallsign.equalsIgnoreCase(connCallsign)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    /**
//     * Attempt to initiate an I-frame connected-mode session from the specified source (usually, the local station)
//     * to another station by sending a SABME frame.
//     * @param src AX25Callsign of originating station (should be a local callsign)
//     * @param dest AX25Callsign of destination for connection
//     * @param via digipeater path (may be null)
//     * @param callback ConnectionEstablishmentListener to be notified of changes in state of the connection
//     * @param sessionIdentifier arbitrary unique identifier for the connection, so that the ConnectionEstablishmentListener
//     *                          can tell which connection is being reported about
//     * @return ConnState for opened connection, or null if connection initiating transmission could not be made
//     * @throws java.net.UnknownServiceException if station configured to not support connected-mode packet protocol
//     * @throws java.io.IOException if connection initiation (SABM frame) could not be sent
//     */
//    public static ConnState openConnection(AX25Callsign src, AX25Callsign dest, AX25Callsign[] via, ConnectionEstablishmentListener callback, Object sessionIdentifier) throws UnknownServiceException, IOException {
//        // confirm that this station can send non-APRS AX.25 frames
//        PortConnector usablePort = null;
//        for (PortConnector connector : getPortList()) {
//            if (connector.hasCapability(Connector.CAP_RCV_PACKET_DATA|Connector.CAP_XMT_PACKET_DATA|Connector.CAP_RAW_AX25)) {
//                usablePort = connector;
//                break;
//            }
//        }
//        if (usablePort == null) {
//            throw new UnknownServiceException("this station does not support non-APRS raw AX.25 packets");
//        }
//
//        // OK, we can theoretically talk to this station, so try opening a connection
//        ConnState state = AX25Stack.getConnState(src, dest, true);
//        if (state.isOpen()) {
//            callback.connectionNotEstablished(sessionIdentifier, new IOException("connection to " + dest + " already open"));
//            return null;
//        }
//        state.setConnector((TransmittingConnector)usablePort);
//        state.transition = ConnState.ConnTransition.LINK_UP;
//        state.setConnType(ConnState.ConnType.MOD128);
//        state.listener = callback;
//        state.sessionIdentifier = sessionIdentifier;
//        if (DebugCtl.isDebug("ax25")) System.out.println("Transmitter.openConnection(" + src + "->" + dest + ',' + Arrays.toString(via) + "): sending SABME U-frame");
//        AX25Frame sabmeFrame = new AX25Frame();
//        sabmeFrame.sender = src.dup();
//        sabmeFrame.dest = dest.dup();
//        sabmeFrame.setCmd(true);
//        sabmeFrame.digipeaters = via;
//        state.via = via;
//        sabmeFrame.ctl = (byte)(AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_SABME);
//        Transmitter transmitter = instance;
//        transmitter.queue(sabmeFrame);
//        state.setResendableFrame(sabmeFrame, transmitter.retransmitCount);
//        return state;
//    }
//
//    /**
//     * Shut down the transmitter.
//     */
//    public void close() {
//        for (PortConnector p : portList) {
//            if (p != null) {
//                p.close();
//            }
//        }
//    }
//
//    /**
//     * Force an immediate flush of the transmit log file.
//     */
//    public void flushLog() {
//        logger.flush();
//    }
//
//    /**
//     * Log a packet to the transmit log. Should only be called by AX25-capable port drivers
//     * when a packet is actually sent out the port.
//     * @param frame already-timestamped AX25Frame that was just transmitted
//     */
//    public void logTransmittedPacket(AX25Frame frame) {
//        logger.log(frame);
//    }
//
//    /**
//     * Log a packet to the transmit log. Should only be called by AX25-capable port drivers
//     * when a packet is actually sent out the port.
//     * @param frame AX25Frame that was just transmitted
//     * @param timestamp long time in milliseconds since UTC epoch when packet was transmitted
//     */
//    public void logTransmittedPacket(AX25Frame frame, long timestamp) {
//        logger.log(frame, timestamp);
//    }
//
//    /**
//     * Class to shut down the Transmitter as a parallel thread.
//     */
//    public static class Close implements Runnable {
//        /**
//         * Thread body to shut down the Transmitter's processing loop.
//         */
//        public void run() {
//            getInstance().close();
//        }
//    }
//}
