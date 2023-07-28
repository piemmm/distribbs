package org.prowl.distribbs.eventbus.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.utils.Tools;

import java.util.Locale;

/**
 * An event fired when one of the RF interfaces receives a packet (which may ba
 * valid, invalid, etc)
 */
public class RxRFPacket extends BaseEvent {

    private static final Log LOG = LogFactory.getLog("RxRFPacket");
    private long compressedByteCount;
    private long uncompressedByteCount;
    private long rxTime;
    private byte[] packet;
    private byte[] compressedPacket;
    private Interface connector;

    // Decoded bits
    private String source;
    private String destination;
    private String command;
    private byte[] payload;
    private double rssi;
    private boolean corrupt = false;

    public RxRFPacket(Interface connector, byte[] compressedPacket, long rxTime, double rssi) {
        super();
        this.rxTime = rxTime;
        this.connector = connector;
        this.packet = null;
        this.compressedPacket = compressedPacket;
        this.rssi = rssi;
        try {
            this.packet = Tools.decompress(compressedPacket);

            compressedByteCount += compressedPacket.length;
            uncompressedByteCount += this.packet.length;

            // Packets should be in the form:
            // source>destination:command:payload
            int chev = Tools.indexOf('>', packet, 0);
            int col = Tools.indexOf(':', packet, 0);
            int colb = Tools.indexOf(':', packet, col + 1);


            if ((packet[0] & 0xFF) == 0x7E) {
                // ax25 enacpsulated
                source = "";
                destination = "";
                command = "";
                payload = new byte[packet.length - 1];
                System.arraycopy(packet, 1, payload, 0, packet.length - 1);
            } else {
                if (chev == -1 || col == -1 || chev > col) {
                    // Invalid packet
                    return;
                }

                // Extract the bits from the packet we want
                source = new String(packet, 0, chev).toUpperCase(Locale.ENGLISH);
                destination = new String(packet, chev + 1, col - (chev + 1)).toUpperCase(Locale.ENGLISH);
                command = null;
                payload = null;
                if (colb != -1) {
                    command = new String(packet, col + 1, colb - (col + 1));
                    payload = new byte[packet.length - (colb + 1)];
                    System.arraycopy(packet, colb + 1, payload, 0, packet.length - (colb + 1));
                }
            }


        } catch (Throwable e) {
            corrupt = true;
            LOG.info("Problem with packet(" + connector.getFrequency() + "):" + Tools.byteArrayToHexString(compressedPacket) + "  uncompressed:" + Tools.byteArrayToHexString(compressedPacket));
        }

    }

    public void setCorrupt() {
        this.corrupt = true;
    }

    public synchronized byte[] getPacket() {
        return packet;
    }

    public byte[] getCompressedPacket() {
        return compressedPacket;
    }

    public Interface getConnector() {
        return connector;
    }

    public long getRxTime() {
        return rxTime;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getCommand() {
        return command;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isCorrupt() {
        return corrupt;
    }

    public double getRSSI() {
        return rssi;
    }

    public long getCompressedByteCount() {
        return compressedByteCount;
    }

    public long getUncompressedByteCount() {
        return uncompressedByteCount;
    }

    public boolean isAX25() {
        if (packet == null || packet.length == 0) {
            return false;
        }
        return ((packet[0] & 0xFF) == 0x7E);
    }

}
