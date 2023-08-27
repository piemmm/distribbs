package org.prowl.distribbs.node.connectivity.agents.fbb;


import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.ConnectionEstablishmentListener;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.node.connectivity.ax25.Interface;
import org.prowl.distribbs.node.connectivity.ax25.Stream;
import org.prowl.distribbs.objects.Storage;
import org.prowl.distribbs.objects.messages.Message;
import org.prowl.distribbs.utils.Tools;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * This FBB sync agent uses the plain text MBL/RLI sync method for use where you countries
 * communcations regulations may require plain text radio comms (which also excludes
 * the ability to use any form of compression)
 * <p>
 * Generally, where the remote node supports it, a method using compression should be used.
 */
public class FBBSyncAgent extends Interface {

    private static final Log LOG = LogFactory.getLog("FBBSyncAgent");
    private static final String CR = "\r";
    private static final int CTRL_Z = 0x1A;

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
        super(config);
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

                LOG.info("Creating listening socket on " + listenPort);
                ServerSocket incomingSocket = new ServerSocket(listenPort);
                while (!quit) {
                    Socket connection = incomingSocket.accept();

                    Tools.runOnThread(() -> {
                        try {
                            newIncomingConnection(connection);
                            connection.close();
                        } catch (Throwable e) {
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

    public void newIncomingConnection(Socket connection) throws IOException {
        try {
            LOG.info("Incoming connection from:" + connection.getInetAddress().getHostAddress());

            InputStream in = connection.getInputStream();
            DataInputStream reader = new DataInputStream((in));
            OutputStream out = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);

            // Remote station connects to use, we vet the username and password, sends [FBB-xxx-somesuch]
            writer.write(CR + DistriBBS.NAME + " BBS, TELNET Access" + CR + CR);
            writer.write("Callsign : ");
            writer.flush();
            String callsign = reader.readLine();

            writer.write("Password : ");
            writer.flush();
            String password = reader.readLine();


            // Rate limit here.
            Tools.delay(2000);

            // Check user/password are ok, if so, continue.
            // TODO FIXME add validation here.
            LOG.info("Remote station identified as:" + callsign);


            // Send prompt.
            writer.write(CR + "Logon Ok." + CR);
            writer.write(CR + "[FBB-5.10-FMH$]" + CR);
            writer.write(DistriBBS.INSTANCE.getMyCall() + " Mailbox." + CR);
            writer.write(DistriBBS.INSTANCE.getMyCall() + ">" + CR);
            writer.flush();

            // Remote station should send [] at this point. If not then kick it off.
            String modeChange = reader.readLine();
            LOG.info("ModeLine:" + modeChange);
            if (!modeChange.startsWith("[")) {
                return;
            }


            // Remote station sends proposal, ends with 'F>'
            while (!quit) {
                String line;
                List<FBBProposal> proposals = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    LOG.debug("Recieve:" + line);
                    if (line.toLowerCase().startsWith("fq")) {
                        LOG.info("Remote station wants to quit");
                        return;
                    }
                    // Remote station sends proposals, ends with 'F>' or reports no messages with 'FF'
                    if (line.toLowerCase().startsWith("f>") || line.toLowerCase().startsWith("ff")) {
                        break;
                    } else if (line.toLowerCase().startsWith("fb")) {
                        FBBProposal proposal = new FBBProposal(line);
                        proposals.add(proposal);
                        LOG.info("Receiving proposal: " + line);
                    } else {
                        LOG.info("Out of spec data received:");
                        // Out of spec protocol.
                        return;
                    }
                }

                // I reply with a load of 'FS' and +,- or = for want, no want, or maybe later for each message in the proposal
                if (proposals.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (FBBProposal proposal : proposals) {
                        // Do I want the BID_MID key? - lets check to see if I have it or not
                        Message searchMessage = new Message();
                        searchMessage.setBID_MID(proposal.getBID_MID());
                        searchMessage.setGroup(proposal.getRecipient());
                        if (storage.doesNewsMessageExist(searchMessage)) {
                            sb.append("-");
                            proposal.setSkip(true);
                        } else {
                            sb.append("+");
                        }
                    }
                    LOG.info("Request list:" + sb.toString());
                    writer.write("FS " + sb.toString() + CR);
                    writer.flush();

                    // Remote end then dumps the messages to me, separated by CTRL-Z
                    for (FBBProposal proposal : proposals) {
                        // Skip any messages we said we were not interested in
                        if (proposal.isSkip()) {
                            continue;
                        }
                        Message message = new Message();
                        message.setSubject(reader.readLine());
                        message.setRoute(proposal.getRoute());
                        message.setBID_MID(proposal.getBID_MID());
                        message.setFrom(proposal.getSender());
                        message.setType(proposal.getType());
                        message.setGroup(proposal.getRecipient());
                        message.setDate(System.currentTimeMillis()); // Every BBS uses the date the message entered the system
                        message.setMessageNumber(storage.getNextMessageID());

                        ByteArrayOutputStream body = new ByteArrayOutputStream();
                        // Should append our header to the message body at the top
                        //R:230711/0800Z @:GB7MNK.#43.GBR.EURO #:25140 [Milton Keynes] $:51407_GB7CIP
                        body.write(stampMessage(message).getBytes());

                        LOG.info("Reading message body...");
                        int b = 0;
                        boolean messageGood = false;
                        while (!quit && b != -1) {
                            b = reader.read();
                            if (b == CTRL_Z) {
                                messageGood = true;
                                break;
                            }
                            if (b != -1) {
                                body.write(b);
                            }
                        }
                        message.setBody(body.toByteArray());

                        // Store the news message if we reached the EOM marker.
                        if (messageGood) {
                            LOG.info("Receiving bulletin: " + message.getFrom() + ": " + message.getSubject() + " (" + body.size() + " bytes)");
                            storage.storeNewsMessage(message);
                        } else {
                            LOG.warn("No EOM received for bulletin: " + message.getFrom() + ": " + message.getSubject() + " (" + body.size() + " bytes) - discarded.");
                        }

                        // Consume linebreak after ctrl-z
                        LOG.info("Message break:" + reader.readLine());
                    }
                }

                // Once all messages sent, then I can send my proposal(ets as above), or FF for nothing then FQ to quit.
                // todo: add message proposals here
                LOG.debug("Sending FF (no more messages for me to send)");
                writer.write("FF" + CR);
                writer.flush();
            }

            // Then fq to quit
            LOG.debug("Sending FQ (All done, time to go.)");
            writer.write("FQ" + CR);
            writer.flush();
            LOG.info("All done - disconnecting");


        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public String stampMessage(Message message) {
        StringBuilder sb = new StringBuilder();

        // Date stamp
        sb.append("R:");
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd/HHmm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sb.append(sdf.format(System.currentTimeMillis()));
        sb.append("Z");

        // Routing address
        sb.append(" @:");
        sb.append(DistriBBS.INSTANCE.getBBSAddress());

        // Message Number (internal on BBS)
        sb.append(" #:");
        sb.append(message.getMessageNumber());
        sb.append("\n");
        return sb.toString();
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


    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException {
        return false;
    }

    @Override
    public void disconnect(Stream currentStream) {

    }

    @Override
    public void cancelConnection(Stream stream) {

    }
}
