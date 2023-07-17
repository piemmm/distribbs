package org.prowl.distribbs.uiremote.text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.uiremote.RemoteClient;
import org.prowl.distribbs.services.user.User;
import org.prowl.distribbs.uiremote.text.parser.CommandParser;
import org.prowl.distribbs.utils.ANSI;

import java.io.*;
import java.util.ResourceBundle;

public class TextClient implements RemoteClient {

    private static final Log LOG = LogFactory.getLog("TextClient");

    private static final String CR = "\r";

    private InputStream in;
    private OutputStream out;
    private User user;
    private CommandParser parser;

    public TextClient(User user, InputStream in, OutputStream out) {
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

            send("[" + DistriBBS.NAME + ":" + DistriBBS.VERSION + ":" + DistriBBS.INSTANCE.getBBSServices() + "]"+CR);
            send(Messages.get("usesColour")+CR+CR);
            send(Messages.get("welcomeNewUser")+CR+CR);
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

    public void send(String data) throws IOException {
        out.write(data.getBytes());
    }

    public void flush() throws IOException{
        out.flush();
    }
}
