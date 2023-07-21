package org.prowl.distribbs.ui;

import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.ClientHandler;
import org.prowl.distribbs.services.bbs.BBSClientHandler;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Switch for different remote client types - at the moment it does not do anything but in the future it may.
 *
 * Local clients are designed for local use, where the connectivity bandwidth is high (a GUI, high speed VT220 console,
 * etc) and round trip latency is low.
 *
 * Remote clients are designed for low bandwidth connections (telnet, low baud packet).
 */
public class RemoteUISwitch {

    /**
     * Take a newly connected user and decide what UI they are getting based on if they are new
     * or if they have set something in their preferences.
     *
     * Generally we would use terminal answerback functionality (^E) however in TNC land, because most things are
     * newline buffered we are not able to use this as the answerback message does not terminate with a newline.
     *
     * @param user The connecting user
     * @param in
     * @param out
     */
    public static void newUserConnected(User user, InputStream in, OutputStream out) {
        ClientHandler client = new BBSClientHandler(user,in,out);
        client.start();
    }

}
