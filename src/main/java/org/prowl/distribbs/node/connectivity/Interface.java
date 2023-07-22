package org.prowl.distribbs.node.connectivity;

import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.sx127x.Modulation;
import org.prowl.distribbs.services.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Interface {

    /**
     * List of services that are registered to this interface as defined in the XML configuration.
     */
    protected List<Service> serviceList = Collections.synchronizedList(new ArrayList<>());


    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract String getName();

    public boolean isAnnounce() {
        return false;
    }

    public int getAnnouncePeriod() {
        return 0;
    }

    public Modulation getModulation() {
        return Modulation.NONE;
    }

    public double getNoiseFloor() {
        return Double.MAX_VALUE;
    }

    public double getRSSI() {
        return 0;
    }

    public int getSlot() {
        return 0;
    }

    public long getTxCompressedByteCount() {
        return 0;
    }

    public long getTxUncompressedByteCount() {
        return 0;
    }

    public long getRxCompressedByteCount() {
        return 0;
    }

    public long getRxUncompressedByteCount() {
        return 0;
    }

    public PacketEngine getPacketEngine() {
        return null;
    }

    public boolean isRF() {
        return false;
    }

    public boolean canSend() {
        return false;
    }

    public boolean sendPacket(TxRFPacket packet) {
        return true;
    }

    public int getFrequency() {
        return 0;
    }


    public void registerService(Service service) {
        serviceList.add(service);
    }

}
