package org.prowl.distribbs.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

public class PacketEngine {

   private static final Log      LOG = LogFactory.getLog("PacketEngine");

   private List<TriggerRunnable> triggerList;

   private Connector             connector;

   public PacketEngine(Connector connector) {
      this.connector = connector;
      triggerList = new LinkedList<>();
      init();
   }
   
   public void init() {
      long announceInterval = Math.max(1000l * 60l * 5l, connector.getAnnouncePeriod()); // minimum 5 minutes
      Timer announceTimer = new Timer();
      announceTimer.schedule(new TimerTask() {
         public void run() {
            if (connector.isAnnounce()) {
               TxRFPacket packet = PacketTools.generateAnnouncePacket();
               packet.setConnector(connector);
              // connector.sendPacket(packet);
            }
         }
      }, 6000, announceInterval);
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
      trigger.setExpiresAt(System.currentTimeMillis() + 6000);
      triggerList.add(trigger);

      TxRFPacket packet = new TxRFPacket(PacketTools.getMyCall(), callsign, PacketTools.PING, time);
      connector.sendPacket(packet);
   }

   /**
    * A packet has been received by the port we montior
    * 
    * @param packet
    */
   public void receivePacket(RxRFPacket packet) {
      
      // Ignore corrupt packets
      if (packet.isCorrupt())
         return;
      
      
      String source = packet.getSource();
      String destination = packet.getDestination();
      String command = packet.getCommand();
      byte[] payload = packet.getPayload();
      
      // Process any matching triggers
      ArrayList<TriggerRunnable> toRemove = new ArrayList<>();
      for (TriggerRunnable trigger : new ArrayList<>(triggerList)) {
                
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
      if (DistriBBS.INSTANCE.getMyCall().equals(destination)) {
         switch (command) {
            // Ping reply
            case PacketTools.PING:
               connector.sendPacket(new TxRFPacket(PacketTools.getMyCall(), source, PacketTools.PONG, payload));
               break;
         }
      }

      // Process any state changes that are addressed to us
      if (DistriBBS.INSTANCE.getMyCall().equals(destination)) {
         // processStateChanges(source, destination, command, payload);
      }

   }

}
