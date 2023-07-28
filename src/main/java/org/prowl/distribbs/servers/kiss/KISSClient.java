package org.prowl.distribbs.servers.kiss;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.KISSFrameEvent;
import org.prowl.distribbs.utils.Tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class KISSClient {
    private static final Log LOG = LogFactory.getLog("KISSClient");


    private Socket clientSocket;

    private OutputStream clientOutput;

    public KISSClient(Socket clientSocket) {
        this.clientSocket = clientSocket;

        // Register this client with the event bus so we can send data to it.
        ServerBus.INSTANCE.register(this);
    }

    public void start() {

        // Get input data and send it to all the other client.
        // We have an opportunity here to validate the data.  We could check for valid KISS frames, etc.
        Tools.runOnThread(() -> {
            try {

                // Get each frame from the client which starts with 0xC0 and ends with 0xC0 with data inbetween.
                InputStream input = clientSocket.getInputStream();
                clientOutput = clientSocket.getOutputStream();
                ByteArrayOutputStream frame = new ByteArrayOutputStream();
                while (true) {
                    int ch = input.read();
                    if (ch == -1) {
                        break;
                    }

                    // End of frame, send to all connected clients.
                    if (ch == 0xC0 && frame.size() > 0) {
                        frame.write(ch);
                        LOG.debug("Frame received from client: " + Tools.byteArrayToHexString(frame.toByteArray()));
                        // Write to all clients.
                        KISSFrameEvent event = new KISSFrameEvent(frame.toByteArray());
                        ServerBus.INSTANCE.post(event);
                        frame.reset();
                        continue;
                    } else if (ch == 0xC0 && frame.size() == 0) {
                        // Start of frame
                    }

                    // Write each byte to the frame.
                    frame.write(ch);
                }

                LOG.info("Client closed the connection");
                try {
                    clientSocket.close();
                } catch (Throwable e) {
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }

            // Deregister for events
            ServerBus.INSTANCE.unregister(this);
        });

    }

    @Subscribe
    public void onServerEvent(KISSFrameEvent frame) {
        // Send data to the client.
        try {
            synchronized (clientOutput) {
                LOG.debug("Frame received from another client: " + Tools.byteArrayToHexString(frame.getData()));
                clientOutput.write(frame.getData());
                clientOutput.flush();
            }
        } catch (IOException e) {
            LOG.warn("Client is no longer connected");
        }
    }

}
