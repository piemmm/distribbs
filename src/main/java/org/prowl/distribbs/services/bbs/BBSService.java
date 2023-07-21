package org.prowl.distribbs.services.bbs;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.Service;

import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * BBS Service
 */
public class BBSService extends Service {

    private static final Log          LOG = LogFactory.getLog("BBSService");

    private boolean stop;

    private HierarchicalConfiguration config;

    private String callsign;
    private String bbsAddress;

    public BBSService(HierarchicalConfiguration config) {
        super(config);
        callsign = config.getString("callsign");
        bbsAddress  = config.getString("bbsAddress");
    }

    public void acceptedConnection(User user, InputStream in, OutputStream out) {
        BBSClientHandler client = new BBSClientHandler(user, in, out);
        client.start();
    }

    public void start() {}

    public void stop() {
        stop = true;
    }

    @Override
    public String getCallsign() {
        return callsign;
    }


}
