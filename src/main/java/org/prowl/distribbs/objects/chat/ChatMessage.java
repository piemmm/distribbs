package org.prowl.distribbs.objects.chat;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.objects.InvalidMessageException;
import org.prowl.distribbs.objects.Packetable;
import org.prowl.distribbs.objects.Priority;
import org.prowl.distribbs.utils.Tools;

public class ChatMessage extends Packetable {

   private static final Log LOG = LogFactory.getLog("ChatMessage");

   private String           from;
   private String           group;
   private String           body;
   private long             date;

   public ChatMessage() {
      // Chat messages need to proliferate quickly through the network as they can
      // be very time critical, therefore they have a high priority.
      priority = Priority.HIGH;
   }

   public String getFrom() {
      return from;
   }

   public void setFrom(String from) {
      this.from = from.toUpperCase(Locale.ENGLISH);
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(String group) {
      this.group = group.toUpperCase(Locale.ENGLISH);
   }

   public String getBody() {
      return body;
   }

   public void setBody(String body) {
      this.body = body;
   }

   public long getDate() {
      return date;
   }

   public void setDate(long date) {
      this.date = date;
   }

   public byte[] toPacket() {

      try (ByteArrayOutputStream bos = new ByteArrayOutputStream(from.length() + body.length() + group.length() + 30);
            DataOutputStream dout = new DataOutputStream(bos)) {

         // String.length measures UTF units, which is no good to use, so we will use the
         // byte array size.
         byte[] groupArray = group.getBytes();
         byte[] fromArray = from.getBytes();
         byte[] bodyArray = body.getBytes();

         // Start off with the date
         dout.writeLong(date);
         
         // Originating and latest paths
         toPacketPaths(dout);

         // Signed int, 4 bytes, easily handled by other systems.
         dout.writeInt(groupArray.length);
         dout.write(groupArray);

         dout.writeInt(fromArray.length);
         dout.write(fromArray);

         dout.writeInt(bodyArray.length);
         dout.write(bodyArray);

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
   public ChatMessage fromPacket(DataInputStream din) throws InvalidMessageException {
      try {

         long date = din.readLong();
         fromPacketPaths(din);
         String group = Tools.readString(din, din.readInt());
         String from = Tools.readString(din, din.readInt());
         String body = Tools.readString(din, din.readInt());

         setDate(date);
         setGroup(group);
         setFrom(from);
         setBody(body);

      } catch (Throwable e) {
         LOG.error("Unable to build message from packet", e);
         throw new InvalidMessageException(e.getMessage(), e);
      }
      return this;
   }

}
