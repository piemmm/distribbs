package org.prowl.distribbs.objectstorage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

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
      // Write it to disk
      storeData(getMailMessageFile(message), message.toPacket());
   }

   public File getMailMessageFile(MailMessage message) {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + MAIL + File.separator + message.getTo() + File.separator + timeToSlot(message.getDate()));
      if (!itemDir.exists()) {
         itemDir.mkdirs();
      }
      return new File(itemDir, filename);
   }

   /**
    * Convenience method for if a message exists already
    * 
    * Checks local storage to see if a mail message already exists
    */
   public boolean doesMailMessageExist(MailMessage message) {
      return getMailMessageFile(message).exists();
   }

   
   
   /**
    * Store a chat message
    * 
    * Path: are: module:chatGroup:date:messageFile
    * 
    * @param message
    */
   public void storeChatMessage(ChatMessage message) throws IOException {
      // Write it to disk
      storeData(getChatMessageFile(message), message.toPacket());
   }

   public File getChatMessageFile(ChatMessage message) {
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator + message.getGroup() + File.pathSeparator + timeToSlot(message.getDate()));
      if (!itemDir.exists()) {
         itemDir.mkdirs();
      }
      return new File(itemDir, filename);
   }

   /**
    * Convenience method for if a message exists already
    * 
    * Checks local storage to see if a chat message already exists
    */
   public boolean doesChatMessageExist(ChatMessage message) {
      return getChatMessageFile(message).exists();
   }

   /**
    * Store a news message
    * 
    * Paths are: module:date:group:messageFile
    * 
    * @param message
    */
   public void storeNewsMessage(NewsMessage message) throws IOException {
      // Write it to disk
      storeData(getNewsMessageFile(message), message.toPacket());
   }

   public File getNewsMessageFile(NewsMessage message) {
      String filename = Long.toString(message.getDate()) + "_" + message.getFrom();
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator + timeToSlot(message.getDate()) + File.pathSeparator + message.getGroup());
      if (!itemDir.exists()) {
         itemDir.mkdirs();
      }
      return new File(itemDir, filename);
   }

   /**
    * Convenience method for if a message exists already
    * 
    * Checks local storage to see if a news message already exists
    */
   public boolean doesNewsMessageExist(NewsMessage message) {
      return getNewsMessageFile(message).exists();
   }

   /**
    * Store an APRS message
    * 
    * Paths are: module:date:{date}_packetFile
    * 
    * @param message
    */
   public void storeAPRSMessage(APRSMessage message) throws IOException {
      // Write it to disk
      storeData(getAPRSMessageFile(message), message.toPacket());
   }

   public File getAPRSMessageFile(APRSMessage message) {
      // Get the location to save the file and make sure the directory structure
      // exists
      String filename = Long.toString(message.getDate()) + "_" + Tools.md5(message.getBody());
      File itemDir = new File(locationDir.getAbsolutePath() + File.separator + APRS + File.separator + timeToSlot(message.getDate()));
      if (!itemDir.exists()) {
         itemDir.mkdirs();
      }
      return new File(itemDir, filename);
   }

   /**
    * Convenience method for if a message exists already
    * 
    * Checks local storage to see if an APRS message already exists
    */
   public boolean doesAPRSMessageExist(APRSMessage message) {
      return getAPRSMessageFile(message).exists();
   }

   /**
    * Convert a time in milliseconds to a directory slot.
    * 
    * @param timeMillis
    * @return
    */
   private final String timeToSlot(long timeMillis) {
      // Split this down into directories about 1 day (86400000millis ish) apart
      String dateStr = Long.toString((int) (timeMillis / 100000000d));
      return dateStr;
   }

   private void storeData(File file, byte[] data) throws IOException {
      // Ensure directory tree exists
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
    * Get a list of news group messages going back as far as date X.
    */
   public File[] listNewsMessages(String group, long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] dates = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator).listFiles();
      if (dates != null) {
         for (File date : dates) {
            try {
               if (Long.parseLong(date.getName()) > earliestDate) {
                  File[] groups = date.listFiles();
                  if (groups != null) {
                     for (File groupf : groups) {
                        if (groupf.getName().equals(group) || group == null) {
                           File[] messages = groupf.listFiles();
                           if (messages != null) {
                              files.addAll(Arrays.asList(messages));
                           }
                        }
                     }
                  }
               }
            } catch (NumberFormatException e) {
               // Ignore the 'not a date' file
               LOG.debug("Ignoring file path:" + date, e);
            }
         }
      }
      return files.toArray(new File[files.size()]);
   }

   /**
    * Get a list of all news group messages going back as far as date X.
    */
   public File[] listNewsMessages(long earliestDate) throws IOException {
      return listNewsMessages(null, earliestDate);
   }

   /**
    * Get a mail messages for a callsign
    */
   public File[] listMailMessages(String callsign, long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] dates = new File(locationDir.getAbsolutePath() + File.separator + MAIL + File.separator + callsign).listFiles();
      if (dates != null) {
         for (File date : dates) {
            try {
               if (Long.parseLong(date.getName()) > earliestDate) {
                  File[] messages = date.listFiles();
                  if (messages != null) {
                     files.addAll(Arrays.asList(messages));
                  }
               }
            } catch (NumberFormatException e) {
               LOG.debug("Invalid file:" + e.getMessage() + "  " + date);
            }
         }
      }
      return files.toArray(new File[files.size()]);
   }

   /**
    * Get a mail messages since a date
    */
   public File[] listMailMessages(long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] callsigns = new File(locationDir.getAbsolutePath() + File.separator + MAIL).listFiles();
      if (callsigns != null) {
         for (File callsign : callsigns) {
            File[] dates = callsign.listFiles();
            if (dates != null) {
               for (File date : dates) {
                  try {
                     if (Long.parseLong(date.getName()) > earliestDate) {
                        File[] messages = date.listFiles();
                        if (messages != null) {
                           files.addAll(Arrays.asList(messages));
                        }
                     }
                  } catch (NumberFormatException e) {
                     LOG.debug("Invalid file:" + e.getMessage() + "  " + date);
                  }
               }
            }
         }
      }
      return files.toArray(new File[files.size()]);
   }

   /**
    * Get a list of known chat groups
    * 
    * @return
    * @throws IOException
    */
   public String[] listChatGroups() throws IOException {
      ArrayList<String> results = new ArrayList<>();
      File[] groups = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator).listFiles();
      if (groups != null) {
         for (File file : groups) {
            if (file.isDirectory()) {
               results.add(file.getName().toUpperCase(Locale.ENGLISH));
            }
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
   public File[] listChatMessages(String chatGroup, long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] dates = new File(locationDir.getAbsolutePath() + File.separator + CHAT + File.separator + chatGroup).listFiles();
      if (dates != null) {
         for (File file : dates) {
            try {
               if (Long.parseLong(file.getName()) > earliestDate) {
                  File[] messages = file.listFiles();
                  if (messages != null) {
                     files.addAll(Arrays.asList(messages));
                  }
               }
            } catch (Throwable e) {
               LOG.debug("Exception accessing chat file:" + file, e);
               // Not a file we can use, so ignore.
            }
         }
      }
      return files.toArray(new File[files.size()]);
   }

   
   /**
    * Get a list of chat messages going back as far as date X.  
    * 
    * @param earliestDate
    * @return an unsorted list of files matching the group, going back as far as
    *         'earliestDate'
    * @throws IOException
    */
   public File[] listChatMessages(long earliestDate) throws IOException {
      ArrayList<File> files = new ArrayList<>();
      File[] groups = new File(locationDir.getAbsolutePath() + File.separator + CHAT).listFiles();
      if (groups != null) {
         for (File chatGroup: groups) {
            File[] dates = chatGroup.listFiles();
            if (dates != null) {
               for (File file : dates) {
                  try {
                     if (Long.parseLong(file.getName()) > earliestDate) {
                        File[] messages = file.listFiles();
                        if (messages != null) {
                           files.addAll(Arrays.asList(messages));
                        }
                     }
                  } catch (Throwable e) {
                     LOG.debug("Exception accessing chat file:" + file, e);
                     // Not a file we can use, so ignore.
                  }
               }
            }
         }
      }
      return files.toArray(new File[files.size()]);
   }

   
   /**
    * Retrieve a chat message
    * 
    * @param f
    * @return
    * @throws IOException
    */
   public ChatMessage loadChatMessage(File f) throws IOException {
      ChatMessage message = new ChatMessage();
      try {
         message.fromPacket(loadData(f));
      } catch (InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }

   /**
    * Retrieve a news message
    * 
    * @param f
    * @return
    * @throws IOException
    */
   public NewsMessage loadNewsMessage(File f) throws IOException {
      NewsMessage message = new NewsMessage();
      try {
         message.fromPacket(loadData(f));
      } catch (InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }

   /**
    * Retrieve an APRS packet
    * 
    * @param f
    * @return
    * @throws IOException
    */
   public APRSMessage loadAPRSMessage(File f) throws IOException {
      APRSMessage message = new APRSMessage();
      try {
         message.fromPacket(loadData(f));
      } catch (InvalidMessageException e) {
         throw new IOException(e);
      }
      return message;
   }

   /**
    * Retrieve a mail message
    * 
    * @param f
    * @return
    * @throws IOException
    */
   public MailMessage loadMailMessage(File f) throws IOException {
      MailMessage message = new MailMessage();
      try {
         DataInputStream din = loadData(f);
         message.fromPacket(din);
      } catch (InvalidMessageException e) {
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
   private DataInputStream loadData(File file) throws IOException {
      return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
   }

   /**
    * Get the node file given its callsign.
    * 
    * @return
    */
   private File getNodePropertiesFile(String callsign) {
      File file = new File(locationDir.getAbsolutePath() + File.separator + "syncstate" + File.separator + callsign + ".properties");
      file.getParentFile().mkdirs();
      return file;

   }

   /**
    * Retrieve a remote nodes sync properties file This keeps a list of things like
    * the latest times we managed to sync to with this node
    */
   public NodeProperties loadNodeProperties(String callsign) {
      Properties properties = new Properties();
      try (FileInputStream in = new FileInputStream(getNodePropertiesFile(callsign))) {
         properties.load(in);
      } catch (Throwable e) {
         LOG.debug("Unable to load properties file, or first connection:" + getNodePropertiesFile(callsign));
      }
      return new NodeProperties(properties);
   }

   /**
    * Save the node properties file which contains the current sync state
    * 
    * @param callsign   The callsign of the remote node
    * @param properties the properties file to save.
    */
   public synchronized void saveNodeProperties(String callsign, NodeProperties nodeProperties) {
      try (FileOutputStream fos = new FileOutputStream(getNodePropertiesFile(callsign))) {
         nodeProperties.getProperties().store(fos, "DistriBBS node properties file");
         fos.flush();
      } catch (Throwable e) {
         LOG.error("Unable to save properties file: " + getNodePropertiesFile(callsign));
      }
   }

}