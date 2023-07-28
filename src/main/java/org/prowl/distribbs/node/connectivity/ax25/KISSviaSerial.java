package org.prowl.distribbs.node.connectivity.ax25;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.ax25.*;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.HeardNodeEvent;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.Service;
import org.prowl.distribbs.utils.Tools;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a KISS type connection with a serial port device
 */
public class KISSviaSerial extends Interface {

    private static final Log LOG = LogFactory.getLog("KISSviaSerial");

    private String port;
    private int dataBits;
    private int stopBits;
    private String parity;
    private int serialBaudRate;
    private String defaultOutgoingCallsign;

    private int pacLen;
    private int baudRate;
    private int maxFrames;
    private int frequency;
    private int retries;

    private BasicTransmittingConnector connector;
    private HierarchicalConfiguration config;
    private boolean running;
    private SerialPort serialPort = null; // The chosen port form our enumerated list.


    public KISSviaSerial(HierarchicalConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() throws IOException {
        running = true;

        // The address and port of the KISS interface we intend to connect to (KISS over Serial)
        port = config.getString("port");
        dataBits = config.getInt("dataBits", 8);
        stopBits = config.getInt("stopBits", 1);
        parity = config.getString("parity", "N");
        serialBaudRate = config.getInt("baudRate", 9600);

        // This is the default callsign used for any frames sent out not using a registered service(with its own call).
        // So if I were to say at node level 'broadcast this UI frame on all interfaces' it would use this callsign.
        // But if a service wanted to do the same, (eg: BBS service sending an FBB list) then it would use the service
        // callsign instead.
        defaultOutgoingCallsign = DistriBBS.INSTANCE.getMyCall();

        // Settings for timeouts, max frames a
        pacLen = config.getInt("pacLen", 120);
        baudRate = config.getInt("channelBaudRate", 1200);
        maxFrames = config.getInt("maxFrames", 3);
        frequency = config.getInt("frequency", 0);
        retries = config.getInt("retries", 6);

        // Check the slot is obtainable.
        if (port.length() < 1) {
            throw new IOException("Configuration problem - port " + port + " needs to be set correctly");
        }

        // Rather than just use the port descriptor, we'll iterate through all the ports so we can at least see
        // what the system has available, so the user is not completely in the dark whe looking at logs.
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort testPort : ports) {
            LOG.debug("Found serial port: " + testPort.getSystemPortName());
            if (testPort.getSystemPortName().equals(port)) {
                LOG.debug(" ** Using serial port: " + testPort.getSystemPortName());
                serialPort = testPort;
            }
        }

        if (serialPort == null) {
            throw new IOException("Configuration problem - port " + port + " needs to be set correctly");
        }

        Tools.runOnThread(() -> {
            setup();
        });
    }

    public void setup() {

        int parityInt = SerialPort.NO_PARITY;
        if (parity.equalsIgnoreCase("E")) {
            parityInt = SerialPort.EVEN_PARITY;
        } else if (parity.equalsIgnoreCase("O")) {
            parityInt = SerialPort.ODD_PARITY;
        }


        serialPort.setBaudRate(serialBaudRate);
        serialPort.setParity(parityInt);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.openPort();
        //serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED | SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        InputStream in = serialPort.getInputStream();
        OutputStream out = serialPort.getOutputStream();

        if (in == null || out == null) {
            LOG.error("Unable to connect to kiss service at: " + port + " - this connector is stopping.");
            return;
        }

        // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
        // not just this one.
        AX25Callsign defaultCallsign = new AX25Callsign(defaultOutgoingCallsign);

        connector = new BasicTransmittingConnector(pacLen, maxFrames, baudRate, retries, defaultCallsign, in, out, new ConnectionRequestListener() {
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
            public boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port) {

                LOG.info("Incoming connection request from " + originator + " to " + state.getDst() + " (" + serviceList.size() + " registered services to check...)");

                for (Service service : serviceList) {
                    if (service.getCallsign() != null && state.getDst().toString().equalsIgnoreCase(service.getCallsign())) {
                        LOG.info("Accepting connection request from " + originator + " to " + state.getDst() + " for service " + service.getName());
                        setupConnectionListener(service, state, originator, port);
                        return true;
                    }
                }
                // Do not accept (possibly replace this with a default handler to display a message in the future?)
                // Maybe use the remoteUISwitch to do it?
                LOG.info("Rejecting connection request from " + originator + " to " + state.getDst() + " as no service is registered for this callsign");
                return false;
            }
        });

        // Tag for debug logs so we know what instance/frequency this connector is
        connector.setDebugTag(Tools.getNiceFrequency(frequency));

        // AX Frame listener for things like mheard lists
        connector.addFrameListener(new AX25FrameListener() {
            @Override
            public void consumeAX25Frame(AX25Frame frame, Connector connector) {
                // Create a node to represent what we've seen - we'll merge this in things like
                // mheard lists if there is another node there so that capability lists can grow
                Node node = new Node(KISSviaSerial.this, frame.sender.toString(), frame.rcptTime, frame.dest.toString(), frame);

                // Determine the nodes capabilities from the frame type and add this to the node
                PacketTools.determineCapabilities(node, frame);

                // Fire off to anything that wants to know about nodes heard
                ServerBus.INSTANCE.post(new HeardNodeEvent(node));
            }
        });

    }


    /**
     * A connection has been accepted therefore we will set it up and also a listener to handle state changes
     *
     * @param state
     * @param originator
     * @param port
     */
    public void setupConnectionListener(Service service, ConnState state, AX25Callsign originator, Connector port) {
        // If we're going to accept then add a listener so we can keep track of this connection state
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

                        service.acceptedConnection(user, in, wrapped);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                tx.start();

            }

            @Override
            public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
                LOG.info("Connection not established from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }

            @Override
            public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
                LOG.info("Connection closed from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }

            @Override
            public void connectionLost(Object sessionIdentifier, Object reason) {
                LOG.info("Connection lost from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }
        };
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
    public int getFrequency() {
        return frequency;
    }


}
