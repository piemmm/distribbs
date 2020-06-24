package org.prowl.distribbs.node.connectivity.sx127x;

import org.prowl.distribbs.eventbus.events.TxRFPacket;

public interface Device {

   public void sendMessage(TxRFPacket packet);

   public int getFrequency();

   public double getNoiseFloor();

   public double getRSSI();

   public int setFrequency(int frequencyHz);

   public double setDeviation(double deviationHz);

   public int setAFCFilter(int afcHz);

   public int setDemodFilter(int demodHz);

   public int setBaud(int baud);
}
