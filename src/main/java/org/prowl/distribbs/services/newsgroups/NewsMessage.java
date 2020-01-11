package org.prowl.distribbs.services.newsgroups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.services.InvalidMessageException;
import org.prowl.distribbs.services.Packetable;
import org.prowl.distribbs.services.Priority;
import org.prowl.distribbs.utils.Tools;

/**
 * A news message is from a user to a newsgroup
 */
public class NewsMessage extends Packetable {

   private static final Log LOG = LogFactory.getLog("Message");

   private long             date;
   private String           from;
   private String           group;
   private String           subject;
   private String           body;

   public NewsMessage() {
      // News messages are background work for nodes so are low priority.
      priority = Priority.LOW;
   }

   public String getFrom() {
      return from;
   }

   public void setFrom(String from) {
      this.from = from.toUpperCase(Locale.ENGLISH);;
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(String group) {
      this.group = group.toUpperCase(Locale.ENGLISH);;
   }

   public long getDate() {
      return date;
   }

   public void setDate(long date) {
      this.date = date;
   }

   public String getSubject() {
      return subject;
   }

   public void setSubject(String subject) {
      this.subject = subject;
   }

   public String getBody() {
      return body;
   }

   public void setBody(String body) {
      this.body = body;
   }

   /**
    * Serialise into a byte array. Keeping the size to a minimum is important.
    * Length, data, length, data format for all the fields.
    * 
    * @return A byte array representing the serialised message
    */
   public byte[] toPacket() {

      try (ByteArrayOutputStream bos = new ByteArrayOutputStream(subject.length() + body.length() + group.length() + 30);
            DataOutputStream dout = new DataOutputStream(bos)) {

         // String.length measures UTF units, which is no good to use, so we will use the
         // byte array size.
         byte[] groupArray = group.getBytes();
         byte[] fromArray = from.getBytes();
         byte[] subjectArray = subject.getBytes();
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

         dout.writeInt(subjectArray.length);
         dout.write(subjectArray);

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
    * Deserialise from a byte array
    * 
    * @param packet The serialised form of the news message
    */
   public NewsMessage fromPacket(byte[] packet) throws InvalidMessageException {

      try (ByteArrayInputStream bin = new ByteArrayInputStream(packet);
            DataInputStream din = new DataInputStream(bin)) {

         long date = din.readLong();

         fromPacketPaths(din);

         String group = Tools.readString(din, din.readInt());
         String from = Tools.readString(din, din.readInt());
         String subject = Tools.readString(din, din.readInt());
         String body = Tools.readString(din, din.readInt());

         setDate(date);
         setGroup(group);
         setFrom(from);
         setSubject(subject);
         setBody(body);

      } catch (Throwable e) {
         LOG.error("Unable to build message from packet", e);
         throw new InvalidMessageException(e.getMessage(), e);
      }
      return this;
   }

}
