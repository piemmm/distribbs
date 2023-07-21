package org.prowl.distribbs.services.http;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.Service;

import java.io.InputStream;
import java.io.OutputStream;

public class HTTP extends Service {

    public HTTP(HierarchicalConfiguration config) {
        super(config);
    }

    public void start() {

    }

    public void stop() {

    }

    public String getCallsign() {
        return null;
    }


    @Override
    public void acceptedConnection(User user, InputStream in, OutputStream out) {

    }

}
