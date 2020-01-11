package org.prowl.distribbs.objectstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.services.InvalidMessageException;
import org.prowl.distribbs.services.aprs.APRSMessage;
import org.prowl.distribbs.services.chat.ChatMessage;
import org.prowl.distribbs.services.messages.MailMessage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.utils.Tools;

/**
 * Storage class allows objects to be stored on disk to keep things simple.
 * 
 * Directory based storage, where parts of the object are split up into pieces
 * and recombined when needed as we have plenty of CPU horsepower.
 */
public class Storage {

   private static final Log          LOG         = LogFactory.getLog("Storage");

   private HierarchicalConfiguration config;

   private File                      locationDir = new File("storage");

   private static final String       NEWS        = "news";
   private static final String       CHAT        = "chat";
   private static final String       APRS        = "aprs";
   private static final String       MAIL        = "mail";
   private static final String       QSL         = "qsl";

   public Storage(HierarchicalConfiguration config) {
      this.config = config;
   }

   /**
    * Store a mail message
    * 
    * Paths are: module:recipientCallsign:date:messagefile
    * 
    * @param message
    */
   public void storeMailMessage(MailMessage message) throws IOException {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + MAIL + File.separator + message.getTo() + File.separator + timeToSlot(message.getDate()));

      // Now write it to disk
      storeData(new File(itemDir, filename), message.toPacket());
   }

   /**
    * Store a chat message
    * 
    * Path: are: module:chatGroup:date:messageFile
    * 
    * @param message
    */
   public void storeChatMessage(ChatMessage message) throws IOException {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator + message.getGroup() + File.pathSeparator + timeToSlot(message.getDate()));

      // Now write it to disk
      storeData(new File(itemDir, filename), message.toPacket());
   }

   /**
    * Store a news message
    * 
    * Paths are: module:date:group:messageFile
    * 
    * @param message
    */
   public void storeNewsMessage(NewsMessage message) throws IOException {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator + timeToSlot(message.getDate()) + File.pathSeparator + message.getGroup());

      // Now write it to disk
      storeData(new File(itemDir, filename), message.toPacket());
   }

   /**
    * Store an APRS message
    * 
    * Paths are: module:date:{date}packetFile
    * 
    * @param message
    */
   public void storeAPRSMessage(APRSMessage message) throws IOException {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + Tools.md5(message.getBody());
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + APRS + File.separator + timeToSlot(message.getDate()));

      // Now write it to disk
      storeData(new File(itemDir, filename), message.toPacket());
   }

   private final String timeToSlot(long timeMillis) {
      // Split this down into directories about 1 day (86400000millis ish) apart
      String dateStr = Long.toString((int) (timeMillis / 100000000d));
      return dateStr;
   }

   private void storeData(File file, byte[] data) throws IOException {

      file.getParentFile().mkdirs();

      if (!file.getParentFile().exists()) {
         throw new IOException("Unable to create directory: " + file.getParentFile().getAbsolutePath());
      }

      // Actually try to save the file
      try (FileOutputStream fos = new FileOutputStream(file)) {
         fos.write(data);
         fos.flush();
         fos.close();
      } catch (Throwable e) {
         throw new IOException("Unable to persist file: " + file.getAbsolutePath());
      }
   }

   /**
    * Get a list of known chat groups
    * 
    * @return
    * @throws IOException
    */
   private String[] listChatGroups() throws IOException {
      ArrayList<String> results = new ArrayList<>();
      File[] groups = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator).listFiles();
      for (File file : groups) {
         if (file.isDirectory()) {
            results.add(file.getName().toUpperCase(Locale.ENGLISH));
         }
      }
      return results.toArray(new String[results.size()]);
   }

   /**
    * Get a list of chat messages going back as far as date X. This may be called
    * repeatedly to retrieve some channel data for inactive channels (going back
    * further each time until something is found)
    * 
    * @param chatGroup
    * @param earliestDate
    * @return an unsorted list of files matching the group, going back as far as
    *         'earliestDate'
    * @throws IOException
    */
   private File[] listChatMessages(String chatGroup, long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] dates = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator + chatGroup).listFiles();
      for (File file : dates) {
         try {
            if (Long.parseLong(file.getName()) > earliestDate) {
               files.addAll(Arrays.asList(file.listFiles()));
            }
         } catch (Throwable e) {
            LOG.debug("Exception accessing chat file:" + file, e);
            // Not a file we can use, so ignore.
         }
      }
      return files.toArray(new File[files.size()]);
   }
   
   /**
    * Retrieve a chat message
    * @param f
    * @return
    * @throws IOException
    */
   public ChatMessage loadChatMessage(File f) throws IOException {
      ChatMessage message = new ChatMessage();
      try {
         message.fromPacket(loadData(f));
      } catch(InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }
   
   /**
    * Retrieve a news message
    * @param f
    * @return
    * @throws IOException
    */
   public NewsMessage loadNewsMessage(File f) throws IOException {
      NewsMessage message = new NewsMessage();
      try {
         message.fromPacket(loadData(f));
      } catch(InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }
   
   /**
    * Retrieve an APRS packet
    * @param f
    * @return
    * @throws IOException
    */
   public APRSMessage loadAPRSMessage(File f) throws IOException {
      APRSMessage message = new APRSMessage();
      try {
         message.fromPacket(loadData(f));
      } catch(InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }
   
   /**
    * Retrieve a mail message
    * @param f
    * @return
    * @throws IOException
    */
   public MailMessage loadMailMessage(File f) throws IOException {
      MailMessage message = new MailMessage();
      try {
         message.fromPacket(loadData(f));
      } catch(InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }

   /**
    * Load a data file
    * 
    * @param file
    * @return
    * @throws IOException
    */
   private byte[] loadData(File file) throws IOException {

      byte[] data = new byte[(int) file.length()];
      try (FileInputStream fin = new FileInputStream(file)) {
         fin.read(data);
      }

      return data;
   }

}