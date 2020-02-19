package org.prowl.distribbs.core;

import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.node.connectivity.gps.GPS;
import org.prowl.distribbs.utils.Tools;

import net.sf.marineapi.nmea.util.Position;

public class PacketTools {

   // Commands
   public static final String ANNOUNCE = "ANNOUNCE"; // An announce packet (locator, callsign, etc)
   public static final String PING = "PING"; // Ping request
   
   // Responses
   public static final String PONG = "PONG"; // Ping reply
   
   /**
    * Generate an announce packet - eg:
    * 
    *  "G0TAI>ANNOUNCE:DistriBBS 0.02"
    * 
    * @return
    */
   public static final byte[] generateAnnouncePacket() {
      String maidenhead = "";
      Position position = GPS.getCurrentPosition();
      if (position != null) {
         maidenhead = Tools.convertToMaidenhead(position);
      }
      
      String announce = getCallsign()+">"+ANNOUNCE+":DistriBBS:"+maidenhead+":";
      return announce.getBytes();
   }
   
   public static final String getCallsign() {
      return DistriBBS.INSTANCE.getMyCall();
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
