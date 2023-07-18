package org.prowl.distribbs.node.connectivity.agents.fbb;


import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.Modulation;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.services.messages.MailMessage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FBBSyncAgent implements Connector {

    private static final Log LOG = LogFactory.getLog("FBBSyncAgent");
    private static final String CR = "\r";

    private HierarchicalConfiguration config;

    private String syncHost;
    private int syncPort;
    private String syncUsername;
    private String syncPassword;
    private int listenPort;
    private boolean quit;
    private long syncIntervalMinutes;
    private Storage storage;


    public FBBSyncAgent(HierarchicalConfiguration config) {

        syncHost = config.getString("address");
        syncPort = config.getInt("port", 6300);
        syncUsername = config.getString("username");
        syncPassword = config.getString("password");

        // Listening side
        listenPort = config.getInt("listenPort", 6300);

        // syncIntervalMinutes = config.getInt("syncIntervalMinutes");

        storage = DistriBBS.INSTANCE.getStorage();

    }

    @Override
    public void start() {
        startIncomingListener();

     //   startSync();
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
                        try {
                            newIncomingConnection(connection);
                            connection.close();
                        } catch(Throwable e) {
                            LOG.error(e.getMessage(), e);
                        }
                    });
                    Tools.delay(1000); // Rate limit.
                }


            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    public void newIncomingConnection(Socket connection) {
        try {
            LOG.info("Incoming connection from:" +connection.getInetAddress().getHostAddress());

            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            OutputStream out = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);

            // Remote station connects to use, we vet the username and password, sends [FBB-xxx-somesuch]
            writer.write(CR+DistriBBS.NAME+" BBS, TELNET Access"+CR);
            writer.write("Callsign:");
            writer.flush();
            String callsign = reader.readLine();

            writer.write("Password:");
            writer.flush();
            String password = reader.readLine();


            // Rate limit here.
            Tools.delay(2000);

            // Check user/password are ok, if so, continue.
            // TODO FIXME add validation here.
            LOG.info("Remote station identified as:" + callsign);


            // Send prompt.
            writer.write(CR+"Logon Ok."+CR);
            writer.write(CR+"[FBB-5.10-FMH$]"+CR);
            writer.write(DistriBBS.INSTANCE+" Mailbox"+CR);
            writer.write(DistriBBS.INSTANCE.getMyCall()+">"+CR);
            writer.flush();

            // Remote station should send [] at this point. If not then kick it off.
            String modeChange = reader.readLine();
            LOG.info("ModeLine:"+modeChange);
            if (!modeChange.startsWith("[")) {
                return;
            }

            // Send new prompt
            writer.write(">"+CR);
            writer.flush();

            String line;
            List<FBBProposal> proposals = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                LOG.info("Line:"+line);
                // Remote station sends proposals, ends with 'F>'
                if (line.toLowerCase().startsWith("f>")) {
                    break;
                } else if (line.toLowerCase().startsWith("fb")) {
                    FBBProposal proposal = new FBBProposal(line);
                    proposals.add(proposal);
                    LOG.info("Receiving proposal: "+line);
                } else {
                    // Out of spec protocol.
                    return;
                }
            }

            // I reply with a load of 'FS' and +,- or = for want, no want, or maybe later for each message in the proposal
            if (proposals.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (FBBProposal proposal : proposals) {
                    // Do I want the BID_MID key? - lets check to see if I have it or not
                    if (storage.doesNewsMessageExist(proposal.getBID_MID())) {
                        sb.append("-");
                    } else {
                        sb.append("+");
                    }
                }
                LOG.info("Sending FS:" + sb.toString());
                writer.write("FS:" +sb.toString()+CR);
                writer.flush();

                // Remote end then dumps the messages to me, separated by CTRL-Z
                for (FBBProposal proposal: proposals) {
                    NewsMessage message = new NewsMessage();
                    message.setSubject(reader.readLine());
                    message.setBID_MID(proposal.getBID_MID());
                    message.setFrom(proposal.getSender());
                    message.setType(proposal.getType());

                    StringBuilder body = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        body.append(line+CR);
                    }
                    message.setBody(body.toString());

                    // Store the news message
                    storage.storeNewsMessage(message);

                    LOG.info("Receiving bulletin: "+message.getFrom()+": "+message.getSubject()+" ("+body.length()+")");
                }

                    // Once all messages sent, then I can send my proposal(ets as above), or FF for nothing then FQ to quit.
                writer.write("FF"+CR);
                writer.write("FQ"+CR);
                writer.flush();
                LOG.info("All done - disconnecting");
            }

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
