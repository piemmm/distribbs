package org.prowl.distribbs.services.aprs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.services.InvalidMessageException;
import org.prowl.distribbs.services.Packetable;
import org.prowl.distribbs.services.Priority;
import org.prowl.distribbs.utils.Tools;

public class APRSMessage extends Packetable {

   private static final Log LOG = LogFactory.getLog("ARPSMessage");

   private byte[]           body;                                  // This is basically the RAW APRS packet.
   private long             date;                                  // The date we got the packet

   public APRSMessage() {
      // APRS messages are somewhat time critical;
      priority = Priority.MEDIUM;
   }

   public byte[] getBody() {
      return body;
   }

   public void setBody(byte[] body) {
      this.body = body;
   }

   public long getDate() {
      return date;
   }

   public void setDate(long date) {
      this.date = date;
   }

   public byte[] toPacket() {

      try (ByteArrayOutputStream bos = new ByteArrayOutputStream(body.length + 30);
            DataOutputStream dout = new DataOutputStream(bos)) {

         // Start off with the date
         dout.writeLong(date);

         // Originating and latest paths
         toPacketPaths(dout);
         
         // And then the RAW APRS message
         dout.writeInt(body.length);
         dout.write(body);
         dout.flush();
         dout.close();
         
         return bos.toByteArray();

      } catch (Throwable e) {
         LOG.error("Unable to serialise message", e);
      }
      return null;
   }

   /**
    * Deserialise a packet into this object
    */
   public APRSMessage fromPacket(DataInputStream din) throws InvalidMessageException {
      try {

         long date = din.readLong();
         fromPacketPaths(din);
         byte[] body = Tools.readBytes(din, din.readInt());

         setDate(date);
         setBody(body);
         

      } catch (Throwable e) {
         LOG.error("Unable to build message from packet", e);
         throw new InvalidMessageException(e.getMessage(), e);
      }
      return this;
   }
}
