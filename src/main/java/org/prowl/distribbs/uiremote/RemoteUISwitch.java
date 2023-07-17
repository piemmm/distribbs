package org.prowl.distribbs.uiremote;

import org.prowl.distribbs.services.user.User;
import org.prowl.distribbs.uiremote.text.TextClient;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Switches between clients the user can access (plain text, ansi, etc) based
 * on their preferences.
 */
public class RemoteUISwitch {

    /**
     * Take a newly connected user and decide what UI they are getting based on if they are new
     * or if they have set something in their preferences.
     *
     * @param user The connecting user
     * @param in
     * @param out
     */
    public static void newUserConnected(User user, InputStream in, OutputStream out) {
        RemoteClient client = new TextClient(user,in,out);
        client.start();
    }

}
