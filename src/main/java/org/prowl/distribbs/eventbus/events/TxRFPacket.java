package org.prowl.distribbs.eventbus.events;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.utils.Tools;


/**
 * Tx Packet.
 * 
 * Bit 0-5 - packet type:
 *                        0=normal
 *                        1=kiss passthrough
 * Bit 6-7   - Compression method:
 *                        0=uncompressed
 *                        1=huffman
 *                        (others reserved for better schemes)
 */
public class TxRFPacket extends BaseEvent {

   private static final Log LOG = LogFactory.getLog("TxRFPacket");
   
   private long compressedByteCount;
   private long uncompressedByteCount;

   private byte[]           packet;
   private byte[]           compressedPacket;
   private Connector        connector;

   private String           source;
   private String           destination;
   private String           command;
   private byte[]           payload;

   public TxRFPacket(String source, String destination, String command, byte[] payload) {
      super();
      // this.connector = connector;
      this.source = source;
      this.destination = destination;
      this.command = command;
      this.payload = payload;

      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();

         if (source.length() > 0) {
            String header = source + ">" + destination + ":" + command;
            if (payload != null) {
               header = header + ":";
            }
            bos.write(header.getBytes());
         } else {
            bos.write(0x7E); // KISS passthrough
         }
         bos.write(payload);
         bos.close();
         this.packet = bos.toByteArray();
         this.compressedPacket = Tools.compress(this.packet);
         
         LOG.info("Compression:" + packet.length +"  to  "+compressedPacket.length);
         
         compressedByteCount+=compressedPacket.length;
         uncompressedByteCount+=this.packet.length;
         
      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }
   }

   public void setConnector(Connector connector) {
      this.connector = connector;
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

   public String getSource() {
      return source;
   }

   public String getDestination() {
      return destination;
   }

   public String getCommand() {
      return command;
   }

   public byte[] getPayload() {
      return payload;
   }

   public long getCompressedByteCount() {
      return compressedByteCount;
   }

   public long getUncompressedByteCount() {
      return uncompressedByteCount;
   }
   
   public boolean isAX25() {
      if (packet == null || packet.length == 0) {
         return false;
      }
      return ((packet[0] & 0xFF) == 0x7E);
   }

}
