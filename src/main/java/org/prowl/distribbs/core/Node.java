package org.prowl.distribbs.core;

/**
 * Holder that represents another node
 * 
 * @return
 */
public class Node {

   private String callsign;
   private long lastHeard;
   
   public Node(String callsign, long lastHeard) {
      this.callsign = callsign;
      this.lastHeard = lastHeard;
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

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((callsign == null) ? 0 : callsign.hashCode());
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
      return true;
   }
   
   
   
}
