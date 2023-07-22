package org.prowl.distribbs.services.bbs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.services.bbs.parser.CommandParser;
import org.prowl.distribbs.services.ClientHandler;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.utils.ANSI;

import java.io.*;

public class BBSClientHandler implements ClientHandler {

    private static final Log LOG = LogFactory.getLog("BBSClient");

    private static final String CR = "\r";

    private InputStream in;
    private OutputStream out;
    private User user;
    private CommandParser parser;
    private boolean colourEnabled = true;

    public BBSClientHandler(User user, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.user = user;
        parser = new CommandParser(this);
    }

    @Override
    public void start() {

        try {
            // Get the input stream and handle incoming data in its own thread.
            Thread t = new Thread(() -> {

                try {
                    InputStreamReader reader = new InputStreamReader(in);
                    BufferedReader bin = new BufferedReader(reader);
                    String inLine;
                    while ((inLine = bin.readLine()) != null) {
                        parser.parse(inLine);
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            });
            t.start();

            send("[" + DistriBBS.NAME + "-" + DistriBBS.VERSION + "-" + DistriBBS.INSTANCE.getBBSServices() + "]"+CR);
            send(ANSI.BOLD_CYAN+Messages.get("usesColour")+CR+ANSI.NORMAL+CR);
            send(Messages.get("welcomeNewUser")+CR);
            parser.sendPrompt();

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }

    }

    public void close() {
        try {
            in.close();
        } catch (Throwable e) {
        }
        try {
            out.close();
        } catch (Throwable e) {
        }
    }

    public void setColourEnabled(boolean enabled) {
        this.colourEnabled = enabled;
    }

    public boolean getColourEnabled() {
        return colourEnabled;
    }

    /**
     * Send ASCII text data to the client - will strip colour codes if user has requested it.
     * @param data
     * @throws IOException
     */
    public void send(String data) throws IOException {


        data = data.replaceAll("[^\\x04-\\xFF]", "?");

        // Strip colour if needed.
        if (colourEnabled) {
            data = ANSI.convertTokensToANSIColours(data);
        } else {
            data = ANSI.stripAnsiCodes(data);
            data = ANSI.stripKnownColourTokens(data);
        }



        out.write(data.getBytes());
    }

    public void flush() throws IOException{
        out.flush();
    }
}
