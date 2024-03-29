package org.prowl.distribbs.services;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.distribbs.objects.user.User;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Service {

    private String name;

    public Service(HierarchicalConfiguration config) {
        name = config.getString("name");
        if (name == null) {
            throw new AssertionError("Service name cannot be null");
        }
    }

    public abstract void start();

    public abstract void stop();

    public String getName() {
        return name;
    }

    public abstract String getCallsign();

    public abstract void acceptedConnection(User user, InputStream in, OutputStream out);

}
