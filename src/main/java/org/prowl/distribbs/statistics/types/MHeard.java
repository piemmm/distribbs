package org.prowl.distribbs.statistics.types;

import com.google.common.eventbus.Subscribe;
import org.prowl.distribbs.core.Capability;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.HeardNode;
import org.prowl.distribbs.eventbus.events.RxRFPacket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MHeard {

    private final LinkedList<Node> heardList;

    public MHeard() {
        heardList = new LinkedList<Node>();
        ServerBus.INSTANCE.register(this);
    }

    public List<Node> listHeard() {
        return new ArrayList<Node>(heardList);
    }

    public void addToFront(Node heard) {
        synchronized (heardList) {
            int index = heardList.indexOf(heard);
            if (index != -1) {
                // Update existing node
                Node oldHeard = heardList.remove(index);
                updateNode(oldHeard, heard);
                heardList.addFirst(oldHeard);
            } else {
                // Add new node to list
                heardList.addFirst(heard);
            }
        }

        // Keep the list at a max of 200 entries.
        if (heardList.size() > 200) {
            heardList.removeLast();
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
        oldNode.setConnector(newNode.getConnector());
        for (Capability c : newNode.getCapabilities()) {
            oldNode.addCapabilityOrUpdate(c);
        }
    }

    @Subscribe
    public void heardNode(HeardNode heardNode) {
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
                Node node = new Node(packet.getConnector(), callsign, packet.getRxTime(), packet.getRSSI());
                // Update the list
                addToFront(node);
            }
        }
    }
}
