package org.prowl.distribbs.node.connectivity.ipv4.events;

import org.prowl.distribbs.eventbus.events.BaseEvent;
import org.prowl.distribbs.node.connectivity.ipv4.IPSyncThread;

/**
 * Fired when a node via an IP connects and has authenticated successfully
 * <p>
 * Used to clear off any older clients that may be in a zombie state.
 *
 * @param callsign The node callsign.
 */
public class IPNodeConnectedEvent extends BaseEvent {

    private String callsign;
    private IPSyncThread ipSyncThread;

    public IPNodeConnectedEvent(String callsign, IPSyncThread thread) {
        this.callsign = callsign;
        this.ipSyncThread = thread;
    }

    public String getCallsign() {
        return callsign;
    }

    public IPSyncThread getIpSyncThread() {
        return ipSyncThread;
    }


}
