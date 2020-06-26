package org.prowl.distribbs.node.connectivity;

public interface RFConnector extends Connector {

   public int setFrequency(int frequencyHz);
   
   public double setDeviation(double deviationHz);
   
   public int setAFCFilter(int afcHz);
   
   public int setDemodFilter(int demodHz);
   
   public int setBaud(int baud);
   
   
}
