package org.prowl.distribbs.eventbus.events;

import java.io.EOFException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

/**
 * An event fired when one of the RF interfaces receives a packet (which may ba
 * valid, invalid, etc)
 */
public class RxRFPacket extends BaseEvent {

   private static final Log LOG = LogFactory.getLog("RxRFPacket");

   private long             rxTime;
   private byte[]           packet;
   private byte[]           compressedPacket;
   private Connector        connector;

   public RxRFPacket(Connector connector, byte[] compressedPacket, long rxTime) {
      super();
      this.rxTime = rxTime;
      this.connector = connector;
      this.packet = null;
      this.compressedPacket = compressedPacket;
   }

   /**
    * Lazy decompress of packet by whatever needs it.
    * 
    * @return
    */
   public synchronized byte[] getPacket() {
      if (packet == null) {
         try {
            packet = Tools.decompress(compressedPacket);
         } catch (EOFException e) {
            LOG.error(e.getMessage(), e);
         }
      }
      return packet;
   }

   public byte[] getCompressedPacket() {
      return compressedPacket;
   }

   public Connector getConnector() {
      return connector;
   }

   public long getRxTime() {
      return rxTime;
   }
}
