package org.prowl.distribbs.uiremote.text;

import org.prowl.distribbs.uiremote.RemoteClient;
import org.prowl.distribbs.services.user.User;

import java.io.InputStream;
import java.io.OutputStream;

public class TextClient implements RemoteClient {

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



    }
}
