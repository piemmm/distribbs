package org.prowl.distribbs.core;

import org.prowl.distribbs.node.connectivity.Connector;

/**
 * Holder that represents another node
 *
 * @return
 */
public class Node {

   /**
    * Reference to the connector the device was heard on
    */
   private Connector connector;

   /**
    * The node callsign
    */
   private String    callsign;

   /**
    * The time it was last heard
    */
   private long      lastHeard;

   /**
    * The signal strength (if applicable), 0 if not.
    */
   private double    rssi;

   public Node(Connector connector, String callsign, long lastHeard, double rssi) {
      this.callsign = callsign;
      this.lastHeard = lastHeard;
      this.rssi = rssi;
      this.connector = connector;
   }

   public String getCallsign() {
      return callsign;
   }

   public long getLastHeard() {
      return lastHeard;
   }

   public void setLastHeard(long lastHeard) {
      this.lastHeard = lastHeard;
   }

   public double getRSSI() {
      return rssi;
   }

   public Connector getConnector() {
      return connector;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((callsign == null) ? 0 : callsign.hashCode());
      result = prime * result + ((connector == null) ? 0 : connector.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Node other = (Node) obj;
      if (callsign == null) {
         if (other.callsign != null)
            return false;
      } else if (!callsign.equals(other.callsign))
         return false;
      if (connector == null) {
         if (other.connector != null)
            return false;
      } else if (!connector.equals(other.connector))
         return false;
      return true;
   }

}
