package org.prowl.distribbs.core;

import net.sf.marineapi.nmea.util.Position;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ka2ddo.ax25.AX25Frame;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.APRSTypes;
import org.prowl.aprslib.parser.Parser;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.gps.GPS;
import org.prowl.distribbs.utils.Tools;

public class PacketTools {

    private static final Log LOG = LogFactory.getLog("PacketTools");


    // Reserved destinations
    public static final String ANNOUNCE = "ANNOUNCE"; // An announce packet (locator, callsign, etc)
    public static final String APRS = "APRS"; // An APRS packet

    // Commands
    public static final String PING = "PING"; // Ping request

    // Responses
    public static final String PONG = "PONG"; // Ping reply

    // KISS ax25 legacy passthrough
    public static final String KISS = ""; // KISS passthrough

    /**
     * Generate an announce packet - eg:
     * <p>
     * "G0TAI>ANNOUNCE:DistriBBS:locator:freetext"
     *
     * @return
     */
    public static TxRFPacket generateAnnouncePacket() {
        String maidenhead = "";
        Position position = GPS.getCurrentPosition();
        if (position != null) {
            maidenhead = Tools.convertToMaidenhead(position);
        }
        TxRFPacket packet = new TxRFPacket(getMyCall(), ANNOUNCE, "DistriBBS", maidenhead.getBytes());
        return packet;
    }

    public static String getMyCall() {
        return DistriBBS.INSTANCE.getMyCall();
    }

    /**
     * Decode the sender callsign of a packet.
     *
     * @param packet
     * @return The callsign of the sender, or null if the packet was somehow malformed.
     */
    public static String decodeFrom(byte[] packet) {
        StringBuilder callsign = new StringBuilder();
        for (int i = 0; i < packet.length; i++) {

            if (packet[i] == '>') {
                return callsign.toString();
            }
            callsign.append((char) packet[i]);
        }
        return null;
    }

    /**
     * Check a packets CRC
     *
     * @param packet
     * @return true if the CRC is valid, false otherwise.
     */
    public static final boolean isValidPacket(byte[] packet) {
        return true;
    }


    public static void determineCapabilities(Node node, AX25Frame frame) {

        byte pid = frame.getPid();
        if (pid == AX25Frame.PID_NOLVL3) {
            boolean isAprs = false;

            try {
                String aprsString = frame.sender.toString() + ">" + frame.dest.toString() + ":" + frame.getAsciiFrame();
                APRSPacket packet = Parser.parse(aprsString);
                isAprs = packet.getType() != APRSTypes.UNSPECIFIED;//packet.isAprs();
            } catch (Throwable e) {
                // Ignore - probably not aprs. or unable to parse MICe
            }
            if (isAprs) {
                node.addCapabilityOrUpdate(new Capability(Node.Service.APRS));
            }
        } else if (pid == AX25Frame.PID_NETROM) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.NETROM));
        } else if (pid == AX25Frame.PID_IP) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.IP));
        } else if (pid == AX25Frame.PID_VJC_TCPIP) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.VJ_IP));
        } else if (pid == AX25Frame.PID_FLEXNET) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.FLEXNET));
        } else if (pid == AX25Frame.PID_OPENTRAC) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.OPENTRAC));
        } else if (pid == AX25Frame.PID_TEXNET) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.TEXNET));
        }


        for (Capability c : node.getCapabilities()) {
            LOG.debug("Node: " + node.getCallsign() + " supports " + c.getService().getName());
        }

    }

}
