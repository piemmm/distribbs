package org.prowl.distribbs.statistics.types;

import com.google.common.eventbus.Subscribe;
import org.prowl.distribbs.core.Capability;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.HeardNodeEvent;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.utils.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MHeard {

    protected final List<Node> heardList;

    public MHeard() {
        heardList = Collections.synchronizedList(new LinkedList<Node>());
        ServerBus.INSTANCE.register(this);
    }

    public List<Node> listHeard() {
        synchronized (heardList) {
            return new ArrayList<Node>(heardList);
        }
    }

    public void addToFront(Node heard) {
        synchronized (heardList) {
            int index = heardList.indexOf(heard);
            if (index != -1) {
                // Update existing node
                Node oldHeard = heardList.remove(index);
                updateNode(oldHeard, heard);
                heardList.add(0,oldHeard);
            } else {
                // Add new node to list
                heardList.add(0,heard);
            }
        }

        // Keep the list at a max of 200 entries.
        if (heardList.size() > 200) {
            heardList.remove(heardList.size() - 1);
        }
    }

    /**
     * Update the existing node with the information from the updated one.
     *
     * @param oldNode
     * @param newNode
     */
    private void updateNode(Node oldNode, Node newNode) {
        oldNode.setLastHeard(newNode.getLastHeard());
        oldNode.setRssi(newNode.getRSSI());
        oldNode.setAnInterface(newNode.getInterface());
        for (Capability c : newNode.getCapabilities()) {
            oldNode.addCapabilityOrUpdate(c);
        }
    }

    @Subscribe
    public void heardNode(HeardNodeEvent heardNode) {
        // Quick validation of the callsign.
        if (!Tools.isValidITUCallsign(heardNode.getNode().getCallsign())) {
            return;
        }

        // Update the heard list with a copy.
        addToFront(new Node(heardNode.getNode()));
    }

    @Subscribe
    public void heardNode(RxRFPacket packet) {

        // Ignore corrupt packets in the heard list.
        if (packet.isCorrupt()) {
            return;
        }

        if (packet.isAX25()) {
            // should probably decode header.
        } else {
            // Get the packet and decode to a node
            String callsign = PacketTools.decodeFrom(packet.getPacket());
            if (callsign != null) {
                Node node = new Node(packet.getConnector(), callsign, packet.getRxTime(), packet.getRSSI(), packet.getDestination());
                // Update the list
                addToFront(node);
            }
        }
    }
}
