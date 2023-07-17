package org.prowl.distribbs.uiremote.text;

 import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 import org.prowl.distribbs.Messages;
 import org.prowl.distribbs.uiremote.RemoteClient;
import org.prowl.distribbs.services.user.User;
 import org.prowl.distribbs.utils.ANSI;

 import java.io.*;
 import java.util.ResourceBundle;

public class TextClient implements RemoteClient {

    private static final Log LOG         = LogFactory.getLog("TextClient");



    private InputStream in;
    private OutputStream out;
    private User user;

    public TextClient(User user, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.user = user;
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

                out.write(("echo:"+ ANSI.BLUE+inLine+ANSI.NORMAL).getBytes());
                out.flush();
            }

        } catch(Exception e) {
            LOG.error(e.getMessage(), e);
        }

        System.out.println("RX finished");
    });
    t.start();


    send(Messages.get("usesColour"));
    send(Messages.get("welcomeNewUser"));



} catch(Throwable e ){
    LOG.error(e.getMessage(),e);
}

    }

    public void send(String data) throws IOException  {
        out.write(data.getBytes());
        out.write('\r');
        out.flush();;
    }
}
