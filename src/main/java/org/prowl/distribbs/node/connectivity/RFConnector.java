package org.prowl.distribbs.node.connectivity;

public abstract class RFConnector extends Interface {

   public abstract double setDeviation(double deviationHz);
   
   public abstract int setAFCFilter(int afcHz);
   
   public abstract int setDemodFilter(int demodHz);
   
   public abstract int setBaud(int baud);
   
   
}
