package org.prowl.distribbs.eventbus.events;

import java.io.EOFException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;

public class TxRFPacket extends BaseEvent {

   private static final Log LOG = LogFactory.getLog("TxRFPacket");

   private byte[]           packet;
   private byte[]           compressedPacket;
   private Connector        connector;

   public TxRFPacket(Connector connector, byte[] compressedPacket) {
      super();
      this.connector = connector;
      this.packet = null;
      this.compressedPacket = compressedPacket; // Compress immediately.
   }

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

}
