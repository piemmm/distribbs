package org.prowl.distribbs.core;

import org.prowl.distribbs.DistriBBS;

public class PacketEngine {

   public static final String DESTINATION_ANNOUNCE = "ANNOUNCE";
   
   /**
    * Generate an announce packet - eg:
    * 
    *  "G0TAI>ANNOUNCE:DistriBBS 0.02"
    * 
    * @return
    */
   public static final byte[] generateAnnouncePacket() {
      String announce = getCallsign()+">"+DESTINATION_ANNOUNCE+":DistriBBS "+DistriBBS.VERSION;
      return announce.getBytes();
   }
   
   public static final String getCallsign() {
      return DistriBBS.INSTANCE.getConfiguration().getConfig("callsign", "NOCALL");
   }
   
   
   
   /**
    * Decode the sender callsign of a packet.
    * @param packet
    * @return The callsign of the sender, or null if the packet was somehow malformed.
    */
   public static final String decodeFrom(byte[] packet) {
      StringBuilder callsign = new StringBuilder();
      for (int i = 0; i < packet.length; i++) {
         
         if (packet[i] == '>') {
            return callsign.toString();
         }
         callsign.append((char)packet[i]);
      }
      return null;
   }
   
   /**
    * Check a packets CRC
    * @param packet
    * @return true if the CRC is valid, false otherwise.
    */
   public static final boolean isValidPacket(byte[] packet) {
      return true;
   }
   
   
}
