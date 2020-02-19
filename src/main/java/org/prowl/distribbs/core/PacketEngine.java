package org.prowl.distribbs.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

public class PacketEngine {

   private static final Log      LOG = LogFactory.getLog("PacketEngine");

   private List<TriggerRunnable> triggerList;

   private Connector             connector;

   public PacketEngine(Connector connector) {
      this.connector = connector;
      triggerList = new LinkedList<>();
   }

   /**
    * Ping a remote host.
    * 
    * @param callsign
    * @param l
    */
   public void ping(String callsign, ResponseListener l) {

      // Setup a trigger for the reply to the ping
      TriggerRunnable trigger = new TriggerRunnable() {

         @Override
         public void run(String source, String destination, String response, byte[] payload) {
            super.run(source, destination, response, payload);
            long pingStart = Long.parseLong(new String(payload));
            long pingEnd = System.currentTimeMillis();
            Response r = new Response();
            r.setFrom(source);
            r.setResponseTime(pingEnd - pingStart);
            l.response(r);
         }

         @Override
         public void runExpired() {
            super.runExpired();
            Response r = new Response();
            r.setFrom(callsign);
            r.setResponseTime(-1); // timeout
            l.response(r);

         }
      };
      byte[] time = Long.toString(System.currentTimeMillis()).getBytes();
      trigger.setTriggerFrom(callsign);
      trigger.setTriggerTo(DistriBBS.INSTANCE.getMyCall());
      trigger.setTriggerPayload(time);
      trigger.setTriggerResponse(PacketTools.PONG);
      trigger.setExpiresAt(System.currentTimeMillis() + 2000);
      triggerList.add(trigger);

      sendPacket(DistriBBS.INSTANCE.getMyCall(), callsign, PacketTools.PING, time);
   }

   /**
    * A packet has been received by the port we montior
    * 
    * @param packet
    */
   public void receivePacket(byte[] packet) {

      // Packets should be in the form:
      // source>destination:command:payload
      int chev = Tools.indexOf('>', packet, 0);
      int col = Tools.indexOf(':', packet, 0);
      int colb = Tools.indexOf(':', packet, col + 1);
 
      if (chev == -1 || col == -1 || chev > col) {
         return; // Invalid packet.
      }

      // Extract the bits from the packet we want
      String source = new String(packet, 0, chev).toUpperCase(Locale.ENGLISH);
      String destination = new String(packet, chev + 1, col-(chev+1)).toUpperCase(Locale.ENGLISH);
      String command = null;
      byte[] payload = null;
      if (colb != -1) {
         command = new String(packet, col + 1, colb-(col+1));
         payload = new byte[packet.length - (colb+1)];
         System.arraycopy(packet, colb+1, payload, 0, packet.length - (colb+1));
      }
  
      // Process any matching triggers
      ArrayList<TriggerRunnable> toRemove = new ArrayList<>();
      for (TriggerRunnable trigger : triggerList) {
                
         if (trigger.expired()) {
            toRemove.add(trigger);
            continue;
         }
         
         boolean match = true;
         if (trigger.getTriggerFrom() != null && !trigger.getTriggerFrom().equals(source))
            match = false;
         if (trigger.getTriggerTo() != null && !trigger.getTriggerTo().equals(DistriBBS.INSTANCE.getMyCall()))
            match = false;
         if (trigger.getTriggerResponse() != null && !trigger.getTriggerResponse().equals(command))
            match = false;
         if (trigger.getTriggerPayload() != null && !Tools.arraysEqual(trigger.getTriggerPayload(), payload))
            match = false;

         // Trigger matches, so run it.
         if (match) {
            toRemove.add(trigger);
            trigger.run(source, destination, command, payload);
         }
      }
      triggerList.removeAll(toRemove);

      // Process any non-state 'ui' style packets
      if (destination.equals(DistriBBS.INSTANCE.getMyCall())) {
         switch (command) {
            // Ping reply
            case PacketTools.PING:
               sendPacket(DistriBBS.INSTANCE.getMyCall(), source, PacketTools.PONG, payload);
               break;
         }
      }

      // Process any state changes that are addressed to us
      if (destination.equals(DistriBBS.INSTANCE.getMyCall())) {
         // processStateChanges(source, destination, command, payload);
      }

   }

   public void sendPacket(String from, String to, String request, byte[] payload) {
      if (connector.canSend()) {
         try {
            String header = from + ">" + to + ":" + request;
            if (payload != null) {
               header = header + ":";
            }
            byte[] toSend = new byte[header.getBytes().length + payload.length];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(header.getBytes());
            bos.write(payload);
            bos.close();
            connector.sendPacket(bos.toByteArray());
         } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
         }
      }
   }
}
