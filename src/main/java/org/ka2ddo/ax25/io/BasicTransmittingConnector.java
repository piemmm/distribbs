package org.ka2ddo.ax25.io;

import org.ka2ddo.ax25.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BasicTransmittingConnector extends Connector implements TransmittingConnector, Transmitting {
    private final byte[] rcvBuf = new byte[4096];
    public static final int PROTOCOL_AX25 = 4;
    private final AX25Stack stack;
    private transient KissEscapeOutputStream.RcvState curState = KissEscapeOutputStream.RcvState.IDLE;
    private int wEnd = 0;
    private transient long frameStartTime = -1L;

    private static final ProtocolFamily[] PROTOCOL_FAMILIES = ProtocolFamily.values();

    /**
     * These four bits contain the KISS device ID to be used in KISS frames sent through this port.
     * This supports the TCP port type when talking to the DireWolf
     * software TNC which can support up to 6 audio devices (and therefore up to 6 device IDs in
     * KISS frames). Conveniently, since these bits weren't used before, the backwards-compatible
     * default KISS device ID is zero.
     */
    public static final int FLAGS_MASK_KISSPORT = 0xF00;

    /**
     * This constant gets the number of bits to shift the above {@link #FLAGS_MASK_KISSPORT} bits right
     * to put them in the least significant bits of an integer value.
     */
    public static final int FLAGS_SHIFT_KISSPORT = 8;
    private int retransmitCount;

    /**
     * This is the default callsign this connector will use for transmitting things like UI frames.
     *
     * It is seperate to the {@link ConnectionRequestListener}.acceptInbound() method which will allow you to
     * listen and respond to other callsigns (or ssids)
     */
    public AX25Callsign defaultCallsign;

    private InputStream in;
    private OutputStream out;
    private KissEscapeOutputStream kos;

    public BasicTransmittingConnector(int pacLen, int maxFrames, int baudRateInBits, AX25Callsign defaultCallsign, InputStream in, OutputStream out, ConnectionRequestListener connectionRequestListener) {
        this.defaultCallsign = defaultCallsign;
        this.out = out;
        kos = new KissEscapeOutputStream(out);
        this.in = in;
        stack = new AX25Stack(pacLen, maxFrames, baudRateInBits);
        startRxThread();
        startTxThread();
        stack.setTransmitting(this);
        stack.setConnectionRequestListener(connectionRequestListener);
    }

    public void addFrameListener(AX25FrameListener l) {
        stack.addAX25FrameListener(l);
    }

    /**
     * Wrapper class for frames delay-queued for transmission.
     */
    private static class TimedQueueEntry implements Comparable<BasicTransmittingConnector.TimedQueueEntry> {
        BasicTransmittingConnector.TimedQueueEntry next = null;
        AX25FrameSource frameSource;
        long dueTime;

        TimedQueueEntry(AX25FrameSource frameSource, long dueTime) {
            this.frameSource = frameSource;
            this.dueTime = dueTime;
        }

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        public int compareTo(BasicTransmittingConnector.TimedQueueEntry o) {
            return Long.signum(dueTime - o.dueTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BasicTransmittingConnector.TimedQueueEntry) {
                BasicTransmittingConnector.TimedQueueEntry tqe = (BasicTransmittingConnector.TimedQueueEntry) obj;
                if (dueTime == tqe.dueTime) {
                    return frameSource.equals(tqe.frameSource);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) dueTime;
        }

        @Override
        public String toString() {
            return "TimedQueueEntry[@" + dueTime + ',' + frameSource + ']';
        }
    }


    private final ArrayList<AX25FrameSource> queue = new ArrayList<>();
    private transient BasicTransmittingConnector.TimedQueueEntry delayQueueHead = null;

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public Connector.PortStats getStats() {
        return new Connector.PortStats();
    }

    @Override
    public void sendFrame(AX25Frame frame) throws IOException {
        queue(frame);
    }

    /**
     * Return base callsign or if an SSID is set, the callsign with the SSID
     * @return the default callsign for this connector
     */
    @Override
    public String getCallsign() {
        return defaultCallsign.getSSID() == 0 ? defaultCallsign.getBaseCallsign() : defaultCallsign.getBaseCallsign()+"-"+defaultCallsign.getSSID();
    }

    @Override
    public int getCapabilities() {
        return CAP_XMT_PACKET_DATA | CAP_RCV_PACKET_DATA;
    }

    @Override
    public boolean hasCapability(int capMask) {
        return (getCapabilities() & capMask) == capMask;
    }

    @Override
    public int getAcceptableProtocolsMask() {
        return PROTOCOL_AX25;
    }

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled PortConnectors.
     *
     * @param entry AX25FrameSource of the frame to be transmitted
     */
    public synchronized void queue(AX25FrameSource entry) {
        queue.add(entry);
        notifyAll();
    }

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled PortConnectors.
     *
     * @param entry      AX25FrameSource of the frame to be transmitted
     * @param timeToSend long time in milliseconds since Unix epoch when packet is to be dequeued and transmitted
     */
    public synchronized void delayedQueue(AX25FrameSource entry, long timeToSend) {
        if (timeToSend <= System.currentTimeMillis()) {
            queue.add(entry); // overdue, send now
        } else {
            BasicTransmittingConnector.TimedQueueEntry tqe = new BasicTransmittingConnector.TimedQueueEntry(entry, timeToSend);
            if (delayQueueHead == null) {
                delayQueueHead = tqe;
            } else if (tqe.compareTo(delayQueueHead) < 0) {
                tqe.next = delayQueueHead;
                delayQueueHead = tqe;
            } else {
                BasicTransmittingConnector.TimedQueueEntry prev = delayQueueHead;
                while (prev.next != null && prev.next.compareTo(tqe) < 0) {
                    prev = prev.next;
                }
                tqe.next = prev.next;
                prev.next = tqe;
            }
        }
        notifyAll();
    }


    /**
     * Test if this callsign is addressed to the local station.
     *
     * @param destCallsign String of AX.25 callsign-SSID to test as a destination
     * @return boolean true if this callsign is for the local station
     */
    public boolean isLocalDest(String destCallsign) {
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

        if (getCallsign() != null && destCallsign.equalsIgnoreCase(getCallsign())) {
            return true;
        }

        return false;
    }

    /**
     * Get the locally-originated message retransmit count.
     *
     * @return retransmit count
     */
    public int getRetransmitCount() {
        return retransmitCount;
    }


    /**
     * Transmit the specified frame out one port.
     *
     * @param entry              the AX25FrameSource implementor that will provide the frames to transmit
     * @param now                current time in Unix milliseconds since 1970 UTC
     * @param p                  the port to transmit through
     * @param incrementXmitCount whether or not this is a new round of transmissions, such that proportional pathing should advance to the next path combination
     * @return boolean true if transmit didn't happen for some reason, such that any next call on the same frame should pass the increment flag again
     */
    private boolean sendForOnePort(AX25FrameSource entry, long now, TransmittingConnector p, boolean incrementXmitCount) {
        AX25Frame[] frames;
        // try all supported protocols if message didn't have a default
        int acceptableProtocolsMask = p.getAcceptableProtocolsMask();
        List<AX25Frame[]> alreadySentFrames = new ArrayList<>();
        proto_loop:
        for (int i = 0; i < PROTOCOL_FAMILIES.length; i++) {
            if ((acceptableProtocolsMask & (1 << i)) != 0) {
                frames = entry.getFrames(incrementXmitCount, PROTOCOL_FAMILIES[i], p.getCallsign());
                if (frames != null && frames.length > 0) {
                    for (AX25Frame[] oldSet : alreadySentFrames) {
                        if (Arrays.equals(oldSet, frames)) {
                            System.out.println("AlreadySentFrame");
                            continue proto_loop;
                        }
                    }
                    alreadySentFrames.add(frames);
                    for (AX25Frame frame : frames) {
                        if (frame != null) {
                            if (frame.rcptTime < 0L) {
                                frame.rcptTime = now;
                            }
                            boolean sentSuccessfully = sendFrame(entry, now, frame, p);
                            incrementXmitCount &= !sentSuccessfully;
                        } else {
                            System.out.println(new Date().toString() + ": received null frame from " + entry);
                        }
                    }
                }
            }
        }
        return incrementXmitCount;
    }

    private int getKISSDeviceIDInCorrectBitsFromConfig() {
        return 0;
        //FIXME
        // return (currentCfg.flags & FLAGS_MASK_KISSPORT) >>> (FLAGS_SHIFT_KISSPORT - 4);
    }

    // Actually write the frame to the outputStream
    public final boolean sendFrame(AX25FrameSource entry, long now, AX25Frame frame, TransmittingConnector p) {
        //   fireTransmitting(true);
        int byteCount;
        try {
            if (frame.sender == null || (frame.sourcePort != null && frame.sourcePort.hasCapability(CAP_IGATE))) {
                frame.sender = new AX25Callsign(p.getCallsign());
                frame.sender.h_c = !frame.dest.h_c;
            }
            kos.resetByteCount();
            out.write(KissEscapeOutputStream.FEND);
            kos.write(getKISSDeviceIDInCorrectBitsFromConfig()); // data frame to selected TNC port (KISS device ID)
            frame.write(kos);
            out.write(KissEscapeOutputStream.FEND);
            out.flush();
            byteCount = kos.getByteCount();
            stats.numXmtBytes += byteCount;
            stats.numXmtFrames++;
            //    Transmitter.getInstance().logTransmittedPacket(frame);
        } catch (Exception e) {
            //  fireTransmitting(false);
            //  fireFailed();
            String detail = e.getMessage();
            if (detail.indexOf("roken pipe") >= 0 || detail.indexOf(" closed") >= 0 || detail.indexOf(" reset") >= 0) {
                //       tryToRestartConnection(detail);
            }
            return false; // no need for TNC write delay when packet send failed
        }
        //      long now = System.currentTimeMillis();
        // int estElapsedTimeUntilPacketisTransmitted = (int)((getRFSendTimePerByte() * (byteCount + 10)));
        //  nextAllowableTransmitTime = now + estElapsedTimeUntilPacketisTransmitted;
        //   fireTransmitting(false);
        return true;
    }

    public final void getbuf(InputStream sis) {
        byte[] shortBuf = new byte[1];
        while (sis != null) {
            int newData;
            try {
                int availBytes;
                if ((availBytes = sis.available()) > 0) {
                    for (int count = 0; count < availBytes; count++) {
                        if (sis == null) {
                            break;
                        }
                        // The read(b[], off, len) method is used instead of read() because
                        // SocketInputStream's internal implementation of read() malloc's a 1-element
                        // byte array and then calls read(b[], off, len) on that array for each
                        // byte. This way, we save CPU time and stop malloc'ing so many tiny
                        // buffer arrays.
                        if (sis.read(shortBuf, 0, 1) > 0) {
                            newData = shortBuf[0] & 0xFF;
                            switch (curState) {
                                case IDLE:
                                    if (KissEscapeOutputStream.FEND == newData) {
                                        curState = KissEscapeOutputStream.RcvState.IN_FRAME;
                                        wEnd = 0;
                                    }
                                    break;
                                case IN_FRAME:
                                    switch (newData) {
                                        case KissEscapeOutputStream.FEND:
                                            // send the just-finished frame up to the next layer
                                            if (wEnd > 1) {
                                                // not just a stream of frame borders....
                                                sendDecodedKissFrameToParser();
                                            }
                                            // reset the receive buffer for the next frame
                                            wEnd = 0;
                                            frameStartTime = -1L;
                                            //   fireReceiving(false);
                                            break;
                                        case KissEscapeOutputStream.FESC:
                                            if (-1L == frameStartTime) {
                                                frameStartTime = System.currentTimeMillis();
                                                // fireReceiving(true);
                                            }
                                            curState = KissEscapeOutputStream.RcvState.IN_ESC;
                                            break;
                                        default:
                                            long now = System.currentTimeMillis();
                                            if (-1L == frameStartTime) {
                                                frameStartTime = now;
                                                // fireReceiving(true);
                                            }
                                            if (wEnd < rcvBuf.length) {
                                                rcvBuf[wEnd++] = (byte) newData;
                                            } else {
                                                // some kind of protocol error, so reset and start over
                                                System.out.println(new Date().toString() + ": receive buffer overflow, must be mode garbling, reset protocol");
                                                wEnd = 0;
                                                //  fireReceiving(false);
                                                curState = KissEscapeOutputStream.RcvState.IDLE;
                                            }
                                            break;
                                    }
                                    break;
                                case IN_ESC:
                                    //stats.numRcvBytes++;
                                    switch (newData) {
                                        case KissEscapeOutputStream.TFEND:
                                            rcvBuf[wEnd++] = (byte) KissEscapeOutputStream.FEND;
                                            break;
                                        case KissEscapeOutputStream.TFESC:
                                            rcvBuf[wEnd++] = (byte) KissEscapeOutputStream.FESC;
                                            break;
                                        default:
                                            rcvBuf[wEnd++] = (byte) newData;
                                            break;
                                    }
                                    curState = KissEscapeOutputStream.RcvState.IN_FRAME;
                                    break;
                            }
                        }
                    }
                } else {
                    Thread.sleep(20L); // stall a little while to keep the CPU from thrashing waiting for bytes
                }
            } catch (SocketException e) {
                //  fireFailed();
                String detail = e.getMessage();
                if (detail.indexOf(" closed") >= 0 || detail.indexOf(" reset") >= 0) {
                    //   tryToRestartConnection(detail);
                }
            } catch (Throwable e) {
                //    stats.numBadRcvFrames++;
                System.out.println(new Date().toString() + ": unhandled exception in KissOverTcpConnector");
                e.printStackTrace(System.out);
                // discard this frame
                curState = KissEscapeOutputStream.RcvState.IDLE;
            }
        }
    }

    public void sendDecodedKissFrameToParser() {
        AX25Frame frame = AX25Frame.decodeFrame(rcvBuf, 1, wEnd - 1, stack);
        //System.out.println("rxFrame:" + frame.toString() + " data:" + frame.getAsciiFrame());
        stack.consumeFrameNow(this, frame);
    }

    public void startRxThread() {
        Thread rx = new Thread(() -> {
            try {
                getbuf(in);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }


        });
        rx.start();
    }

    public void startTxThread() {
        Thread tx = new Thread(() -> {
            AX25FrameSource entry = null;
            try {
                while (true) {
                    try {
                        entry = null;
                        synchronized (this) {
                            if (delayQueueHead == null) {
                                while (queue.size() == 0) {
                                    try {
                                        wait();
                                    } catch (InterruptedException e) {
                                        // do nothing, we expect to be interrupted
                                    }
                                }
                            } else {
                                long now = System.currentTimeMillis();
                                do {
                                    if (now < delayQueueHead.dueTime) {
                                        try {
                                            wait(delayQueueHead.dueTime - now);
                                        } catch (InterruptedException e) {
                                            // do nothing, we expect to be interrupted
                                        }
                                        now = System.currentTimeMillis();
                                    }
                                } while (queue.size() == 0 && delayQueueHead != null && now < delayQueueHead.dueTime);
                            }
                            if (queue.size() > 0) {
                                entry = queue.remove(0);
                            } else if (delayQueueHead != null) {
                                if (delayQueueHead.dueTime <= System.currentTimeMillis()) {
                                    entry = delayQueueHead.frameSource;
                                    delayQueueHead = delayQueueHead.next;
                                }
                            }
                        }
                        if (entry != null) {

                            long now = System.currentTimeMillis();
//                            PortConnector p = (PortConnector) entry.getConnector();
//                            boolean incrementXmitCount = true;
//                            BeaconData beaconData = null;
//                            if (entry instanceof BeaconData) {
//                                beaconData = (BeaconData) entry;
//                            }
                            if (hasCapability(Connector.CAP_XMT_PACKET_DATA)) {
                                sendForOnePort(entry, now, this, true);
                            }
                        }
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable e) {
                        if (entry != null) {
                            System.out.println("*** unhandled exception in TransmitterThread with entry type " + entry.getClass().getSimpleName() + ": " + entry);
                        } else {
                            System.out.println("*** unhandled exception in TransmitterThread with null entry");
                        }
                        e.printStackTrace(System.out);
                    }
                }
            } finally {
                System.out.println(new Date().toString() + ": terminating TransmitterThread");
            }
        });
        tx.start();
    }


    /**
     * Set some useful debug information to be included in log messages to identify this connector/stack from others
     * @param tag
     */
    public void setDebugTag(String tag) {
        stack.setDebugTag(tag);
    }

}

