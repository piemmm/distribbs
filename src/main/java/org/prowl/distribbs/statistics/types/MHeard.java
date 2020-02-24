package org.prowl.distribbs.statistics.types;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;

import com.google.common.eventbus.Subscribe;

public class MHeard {

   private LinkedList<Node> heardList;
   
   public MHeard() {
      heardList = new LinkedList<Node>();
      ServerBus.INSTANCE.register(this);
   }
   
   public List<Node> listHeard() {
      return new ArrayList<Node>(heardList);
   }
   
   public void addToFront(Node heard) {
      heardList.remove(heard);
      heardList.addFirst(heard);
      
      // Keep the list at a max of 200 entries.
      if (heardList.size() > 200) {
         heardList.removeLast();
      }
   }
   
   @Subscribe
   public void heardNode(RxRFPacket packet) {
      
      // Ignore corrupt packets in the heard list.
      if (packet.isCorrupt()) {
         return;
      }
      
      // Get the packet and decode to a node
      String callsign = PacketTools.decodeFrom(packet.getPacket());
      if (callsign != null) {
         Node node = new Node(callsign, packet.getRxTime());
         // Update the list
         addToFront(node);
      }
   }
}
