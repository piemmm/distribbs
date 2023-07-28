package org.prowl.distribbs.services.netrom;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.Service;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The netrom service implements a NET/ROM node.
 */
public class NetRomService extends Service {

    private String nodeCallsign;
    private String nodeAlias;

    public NetRomService(HierarchicalConfiguration config) {
        super(config);
        nodeCallsign = config.getString("callsign");
        nodeAlias = config.getString("alias");
    }

    @Override
    public void start() {


    }

    @Override
    public void stop() {

    }

    @Override
    public String getCallsign() {
        return nodeCallsign;
    }

    @Override
    public void acceptedConnection(User user, InputStream in, OutputStream out) {

    }
}
