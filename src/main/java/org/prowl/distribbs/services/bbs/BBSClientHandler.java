package org.prowl.distribbs.services.bbs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.ClientHandler;
import org.prowl.distribbs.services.bbs.parser.CommandParser;
import org.prowl.distribbs.utils.ANSI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BBSClientHandler implements ClientHandler {

    private static final Log LOG = LogFactory.getLog("BBSClient");

    private static final String CR = "\r";

    private volatile InputStream in;
    private volatile OutputStream out;
    private User user;
    private CommandParser parser;
    private boolean colourEnabled = true;
    private BufferedReader bin;

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
                    while (in != null) {
                        StringBuffer inLine = new StringBuffer();
                        int b;
                        while (true) {
                            if (in.available() > 0) {
                                b = in.read();
                                if (b == -1) {
                                    break;
                                }

                                if (b == 13) {
                                    parser.parse(inLine.toString());
                                    inLine.delete(0, inLine.length());
                                } else {
                                    inLine.append((char) b);
                                }

                            } else {
                                // Crude.
                                Thread.sleep(100);
                            }


                        }

                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    close();
                }
            });
            t.start();

            // Send the capabilites header which explains to any client that can interpret it what we are capable of
            // This should be documented on http://wiki.oarc.something.other.if.it.is.accepted
            // If the client sees this, and decides to use them then it should send a [OARC <capabilities>] header containing
            // items previosly seen in our list, to enable them in one go (they take effect immediately)
            send("[EXTN " + DistriBBS.INSTANCE.getStationCapabilities() + "]" + CR);
            flush();

            // Everything else is just part of our standard welcome message
            send(ANSI.BOLD_CYAN + Messages.get("usesColour") + CR + ANSI.NORMAL + CR);
            send(Messages.get("welcomeNewUser") + CR);
            parser.sendPrompt();

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }

    }

    public void useNewInputStream(InputStream in) {
        this.in = in;
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

    public boolean getColourEnabled() {
        return colourEnabled;
    }

    public void setColourEnabled(boolean enabled) {
        this.colourEnabled = enabled;
    }

    /**
     * Send ASCII text data to the client - will strip colour codes if user has requested it.
     *
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

    public void flush() throws IOException {
        out.flush();
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }
}
