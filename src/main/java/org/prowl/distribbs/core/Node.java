package org.prowl.distribbs.core;

import org.prowl.distribbs.node.connectivity.Interface;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder that represents another node
 *
 * @return
 */
public class Node {

   /**
    * Reference to the connector the device was heard on
    */
   private Interface connector;

   /**
    * The node callsign
    */
   private String    callsign;

   /**
    * The time it was last heard
    */
   private long      lastHeard;

   private List<Capability> capabilities = new ArrayList<>();

   /**
    * Enum represenging the station type
    */
   public enum Service {
      BBS("BBS"),
      NETROM("NET/ROM"),
      APRS("APRS"), // APRS transmits are NOLVL3 as well.
      FLEXNET("FLEXNET"),
      OPENTRAC("OPENTRAC"),
      TEXNET("TEXNET"),
      NOLVL3("UI"), // Generic station
      VJ_IP("VJ-TCP/IP"),

      IP("TCP/IP"); // TCP/IP
      private String name;
      Service(String name) {
         this.name = name;
      }
      public String getName() {
         return name;
      }
   }



   /**
    * The signal strength (if applicable), 0 if not.
    */
   private double    rssi = Double.MAX_VALUE;

   public Node(Interface connector, String callsign, long lastHeard, double rssi) {
      this.callsign = callsign;
      this.lastHeard = lastHeard;
      this.rssi = rssi;
      this.connector = connector;
   }

   /**
    * Create a node object, no signal strength information present
    * @param connector
    * @param callsign
    * @param lastHeard
    */
   public Node(Interface connector, String callsign, long lastHeard) {
      this.callsign = callsign;
      this.lastHeard = lastHeard;
      this.connector = connector;
   }

   /**
    * Create a copy of the supplied node.
    * @param toCopy
    */
   public Node(Node toCopy) {
      this.callsign = toCopy.callsign;
      this.lastHeard = toCopy.lastHeard;
      this.rssi = toCopy.rssi;
      this.connector = toCopy.getConnector();
      this.capabilities = new ArrayList<>(toCopy.capabilities);
   }

   /**
    * Add a capability if it's not there, otherwise update the last seen.
    * @param capability
    */
   public void addCapabilityOrUpdate(Capability capability) {
      for (Capability cap: capabilities) {
         if (cap.getService() == capability.getService()) {
            cap.setLastSeen(capability.getLastSeen());
            return;
         }
      }
      capabilities.add(capability);
   }

   /**
    * @return a copy of the array list containing current capabilities
    */
   public List<Capability> getCapabilities() {
      return new ArrayList<>(capabilities);
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

   public Interface getConnector() {
      return connector;
   }

   public void setConnector(Interface connector) {
      this.connector = connector;
   }

   public double getRssi() {
      return rssi;
   }

   public void setRssi(double rssi) {
      this.rssi = rssi;
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
