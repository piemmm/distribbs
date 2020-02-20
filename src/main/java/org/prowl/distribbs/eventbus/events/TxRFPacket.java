package org.prowl.distribbs.eventbus.events;

import java.io.ByteArrayOutputStream;
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
   
   private String           source;
   private String           destination;
   private String           command;
   private byte[]           payload;

   public TxRFPacket(String source, String destination, String command, byte[] payload) {
      super();
      //this.connector = connector;
      this.source = source;
      this.destination = destination;
      this.command = command;
      this.payload = payload;
      
      try {
         String header = source + ">" + destination + ":" + command;
         if (payload != null) {
            header = header + ":";
         }
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         bos.write(header.getBytes());
         bos.write(payload);
         bos.close();
         this.packet = bos.toByteArray();
         this.compressedPacket = Tools.compress(this.packet);
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
   
   

}
