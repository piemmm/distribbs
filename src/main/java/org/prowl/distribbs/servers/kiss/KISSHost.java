package org.prowl.distribbs.servers.kiss;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.utils.Tools;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Creates a listening connection on a port for multiple KISS clients to connect to over TCP.
 * Each kiss client will see each others data. This allows a simple test environment rather than using radios
 * and TNCs to test KISS clients
 */
public class KISSHost {

    private static final Log LOG = LogFactory.getLog("KISSHost");


    // The port to use
    private int port;

    // The listening socket
    private ServerSocket listeningSocket;

    public KISSHost(int port) {
        this.port = port;

    }

    /**
     * Start the listening server
     */
    public void start() {

        try {
            // create the listening socket
            listeningSocket = new ServerSocket(port);

            // start the listening thread, listen for incoming connections and then spawn a client thread for each connection.
            Tools.runOnThread(() -> {
                try {

                    Socket incoming = null;
                    while ((incoming = listeningSocket.accept()) != null) {

                        LOG.info("Incoming connection from " + incoming.getInetAddress().getHostAddress());

                        // create a new client thread for each connection
                        KISSClient client = new KISSClient(incoming);
                        client.start();
                    }

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        KISSHost host = new KISSHost(8001);
        host.start();
    }

}
