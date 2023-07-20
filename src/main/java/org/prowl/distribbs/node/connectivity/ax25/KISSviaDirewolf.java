package org.prowl.distribbs.node.connectivity.ax25;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ka2ddo.ax25.*;
import org.ka2ddo.ax25.io.BasicTransmittingConnector;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.Parser;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Capability;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.HeardNode;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;
import org.prowl.distribbs.services.user.User;
import org.prowl.distribbs.statistics.types.MHeard;
import org.prowl.distribbs.uiremote.RemoteUISwitch;
import org.prowl.distribbs.utils.Tools;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Implements a KISS type passthrough on a fifo file so that things like
 * ax25-tools can play with it.
 * <p>
 * Data is forwarded and received on the designated rf slot (where an SX1278
 * usually resides)
 */
public class KISSviaDirewolf implements Connector {

    private static final Log LOG = LogFactory.getLog("KISSviaDirewolf");

    private String address;
    private int port;
    private String callsign;

    private int pacLen;
    private int maxFrames;
    private int baudRate;

    private BasicTransmittingConnector connector;


    private HierarchicalConfiguration config;
    private boolean running;

    public KISSviaDirewolf(HierarchicalConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() throws IOException {
        running = true;
        address = config.getString("address");
        port = config.getInt("port");
        callsign = config.getString("callsign");

        pacLen = config.getInt("pacLen",120);
        baudRate = config.getInt("channelBaudRate",1200);
        maxFrames = config.getInt("maxFrames",3);


        // Check the slot is obtainable.
        if (port < 1) {
            throw new IOException("Configuration problem - port " + port + " needs to be greater than 0");
        }

        Tools.runOnThread(() -> {
            setup();
        });
    }

    public void setup() {
        try {
            LOG.info("Connecting to kiss service at: " + address+":"+port);
            Socket s = new Socket(InetAddress.getByName(address), port);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            LOG.info("Connected to kiss service at: " + address+":"+port);

            // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
            // not just this one.
            AX25Callsign defaultCallsign = new AX25Callsign(callsign);

            connector = new BasicTransmittingConnector(pacLen, maxFrames, baudRate, defaultCallsign, in, out, new ConnectionRequestListener() {

                /**
                 * Determine if we want to respond to this connection request (to *ANY* callsign) - usually we only accept
                 * if we are interested in the callsign being sent a connection request.
                 *
                 * @param state      ConnState object describing the session being built
                 * @param originator AX25Callsign of the originating station
                 * @param port       Connector through which the request was received
                 * @return
                 */
                @Override
                public boolean acceptInbound(ConnState state, AX25Callsign originator, org.ka2ddo.ax25.Connector port) {

                    LOG.info("Incoming connection request from " + originator + " to " + state.getDst());

                    // If we're going to accept then add a listener so we can keep track of the connection
                    state.listener = new ConnectionEstablishmentListener() {
                        @Override
                        public void connectionEstablished(Object sessionIdentifier, ConnState conn) {

                            Thread tx = new Thread(() -> {
                                // Do inputty and outputty stream stuff here
                                try {
                                    User user = DistriBBS.INSTANCE.getStorage().loadUser(conn.getSrc().getBaseCallsign());
                                    InputStream in = state.getInputStream();
                                    OutputStream out = state.getOutputStream();

                                    // This wrapper provides a simple way to terminate the connection when the outputstream
                                    // is also closed.
                                    OutputStream wrapped = new BufferedOutputStream(out) {
                                        @Override
                                        public void close() throws IOException {
                                            conn.close();
                                        }

                                    };

                                    RemoteUISwitch.newUserConnected(user, in, wrapped);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            tx.start();
                        }

                        @Override
                        public void connectionNotEstablished(Object sessionIdentifier, Object reason) {

                        }

                        @Override
                        public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {

                        }

                        @Override
                        public void connectionLost(Object sessionIdentifier, Object reason) {

                        }
                    };
                    return true;
                }


            });

            // AX Frame listener for things like mheard lists
            connector.addFrameListener(new AX25FrameListener() {
                @Override
                public void consumeAX25Frame(AX25Frame frame, org.ka2ddo.ax25.Connector connector) {
                    // Create a node to represent what we've seen - we'll merge this in things like
                    // mheard lists if there is another node there so that capability lists can grow
                    Node node = new Node(KISSviaDirewolf.this, frame.sender.toString(), frame.rcptTime );

                    // Determine the nodes capabilities from the frame type and add this to the node
                    PacketTools.determineCapabilities(node, frame);

                    // Fire off to anything that wants to know about nodes heard
                    ServerBus.INSTANCE.post(new HeardNode(node));
                }
            });

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        ServerBus.INSTANCE.unregister(this);
        running = false;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isAnnounce() {
        return false;
    }

    @Override
    public int getAnnouncePeriod() {
        return 0;
    }

    @Override
    public Modulation getModulation() {
        return Modulation.NONE;
    }

    @Override
    public PacketEngine getPacketEngine() {
        return null;
    }

    @Override
    public boolean isRF() {
        return false;
    }

    @Override
    public boolean canSend() {
        return false;
    }

    @Override
    public boolean sendPacket(TxRFPacket packet) {
        return true;
    }

    @Override
    public int getFrequency() {
        return 0;
    }

    @Override
    public double getNoiseFloor() {
        return 0;
    }

    @Override
    public double getRSSI() {
        return 0;
    }

    public int getSlot() {
        return 0;
    }

    @Override
    public long getTxCompressedByteCount() {
        return 0;
    }

    @Override
    public long getTxUncompressedByteCount() {
        return 0;
    }

    @Override
    public long getRxCompressedByteCount() {
        return 0;
    }

    @Override
    public long getRxUncompressedByteCount() {
        return 0;
    }

}
