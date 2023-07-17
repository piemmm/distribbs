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

    private static final void newUser(User user, InputStream in, OutputStream out) {

        RemoteClient client = new TextClient(user,in,out);
        client.start();


    }

}
