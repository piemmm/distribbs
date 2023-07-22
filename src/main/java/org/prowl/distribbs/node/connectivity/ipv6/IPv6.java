package org.prowl.distribbs.node.connectivity.ipv6;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.core.PacketEngine;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.node.connectivity.sx127x.Modulation;

import java.io.IOException;

public class IPv6 extends Interface {

    private static final Log LOG = LogFactory.getLog("IPv6");

    private HierarchicalConfiguration config;
    private String sharedSecret;

    public IPv6(HierarchicalConfiguration config) {
        this.config = config;
    }

    public void start() throws IOException {
        sharedSecret = config.getString("sharedSecret");

    }

    public void stop() {

    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public boolean isAnnounce() {
        return false;
    }

    public int getAnnouncePeriod() {
        return 0;
    }

    public Modulation getModulation() {
        return Modulation.NONE;
    }

    public boolean isRF() {
        return false;
    }

    public boolean canSend() {
        return true;
    }

    public boolean sendPacket(TxRFPacket packet) {
        return false;
    }

    @Override
    public PacketEngine getPacketEngine() {
        return null;
    }

    public double getNoiseFloor() {
        return 0;
    }

    public double getRSSI() {
        return 0;
    }

    public int getFrequency() {
        return 0;
    }

    @Override
    public int getSlot() {
        return -1;
    }


    @Override
    public long getTxCompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTxUncompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getRxCompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getRxUncompressedByteCount() {
        // TODO Auto-generated method stub
        return 0;
    }

}
