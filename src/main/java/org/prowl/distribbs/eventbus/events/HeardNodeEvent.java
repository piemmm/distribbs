package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.core.Node;

public class HeardNodeEvent extends BaseEvent {

    private final Node node;

    public HeardNodeEvent(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

}
