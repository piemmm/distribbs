package org.ka2ddo.ax25;


import org.prowl.ax25.*;
import org.prowl.distribbs.ax25.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockTransmitting extends Connector implements TransmittingConnector, Transmitting {

    private List<AX25Frame> sentFrames = new ArrayList<>();

    public MockTransmitting() {

    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void queue(AX25FrameSource entry) {

    }

    @Override
    public void delayedQueue(AX25FrameSource entry, long timeToSend) {

    }

    @Override
    public boolean isLocalDest(String destCallsign) {
        return false;
    }

    @Override
    public int getRetransmitCount() {
        return 0;
    }

    @Override
    public void sendFrame(AX25Frame frame) throws IOException {
        sentFrames.add(frame);
    }

    public List<AX25Frame> getSentFrames() {
        return sentFrames;
    }

    @Override
    public int getAcceptableProtocolsMask() {
        return 0;
    }
}
