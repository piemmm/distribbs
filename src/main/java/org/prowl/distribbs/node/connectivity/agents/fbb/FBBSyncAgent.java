package org.prowl.distribbs.node.connectivity.agents.fbb;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;
import org.prowl.distribbs.utils.Tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FBBSyncAgent implements Connector {

    private static final Log LOG = LogFactory.getLog("FBBSyncAgent");

    private HierarchicalConfiguration config;

    private String syncHost;
    private int syncPort;
    private String syncUsername;
    private String syncPassword;
    private int listenPort;
    private boolean quit;
    private long syncIntervalMinutes;


    public FBBSyncAgent(HierarchicalConfiguration config) {

        syncHost = config.getString("address");
        syncPort = config.getInt("port", 6300);
        syncUsername = config.getString("username");
        syncPassword = config.getString("password");

        // Listening side
        listenPort = config.getInt("listenPort", 6300);

        // syncIntervalMinutes = config.getInt("syncIntervalMinutes");
    }

    @Override
    public void start() {
        startIncomingListener();

        startSync();
    }

    @Override
    public void stop() {
        quit = true;
    }

    public void startIncomingListener() {

        Tools.runOnThread(() -> {

            try {

                LOG.info("Creating listening socket on "+listenPort);
                ServerSocket incomingSocket = new ServerSocket(listenPort);
                while (!quit) {
                    Socket connection = incomingSocket.accept();

                    Tools.runOnThread(() -> {
                        newIncomingConnection(connection);
                    });

                }


            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    public void newIncomingConnection(Socket connection) {
        try {
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            // Remote station connects to use, we vet the username and password

            // Remote station sends proposals, ends with 'F>'


            // I reply with a load of 'FS' and +,- or = for want, no want, or maybe later for each message in the proposal

            // Remote end then dumps the messages to me, separated by CTRL-Z

            // Once all messages sent, then I can send my proposal(ets as above), or FF for nothing then FQ to quit.


        } catch(Throwable e) {
            LOG.error(e.getMessage(),e);
        }
    }

    /**
     * Actually do a sync with the configured remote host
     */
    public void startSync() {

        Tools.runOnThread(() -> {

            try {
                LOG.info("Connecting to FBB host: " + syncHost + ":" + syncPort);
                Socket s = new Socket(InetAddress.getByName(syncHost), syncPort);
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                LOG.info("Connected to FBB host: " + syncHost + ":" + syncPort);

                // Wait for 'call'

                // Wait for 'pass'


                // Logged in, send '[FBB-5.10-MH$]' for basic ascii protocol use


                // Send proposal


                // Get messages wanted


                // Send messages, each separated by CTRL-Z


                // Now it sends it's propsal, or FF for no messages then FQ and disconnect


                // Finish the sync
                s.close();
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        });

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
