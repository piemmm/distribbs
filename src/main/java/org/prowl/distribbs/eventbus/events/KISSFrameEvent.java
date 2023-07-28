package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.servers.kiss.KISSClient;

/**
 * Represents a KISS frame containing KISS data
 */
public class KISSFrameEvent extends BaseEvent {

    private byte[] data;

    private KISSClient source;

    public KISSFrameEvent(byte[] data, KISSClient source) {
        this.data = data;
        this.source = source;
    }

    public byte[] getData() {
        return data;
    }

    public KISSClient getSource() {
        return source;
    }

}
