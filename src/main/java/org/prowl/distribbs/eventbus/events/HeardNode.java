package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.core.Node;

public class HeardNode extends BaseEvent {

    private final Node node;

    public HeardNode(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

}
