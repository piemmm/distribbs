package org.prowl.distribbs.objectstorage;

import java.util.Locale;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NodeProperties {
   
   private static final Log    LOG          = LogFactory.getLog("NodeProperties");

   public static final String LAST_SYNC_MAIL = "lastSyncMail";
   public static final String LAST_SYNC_NEWS = "lastSyncNews";
   public static final String LAST_SYNC_APRS = "lastSyncAPRS";
   public static final String LAST_SYNC_CHAT = "lastSyncChat";
   
   private Properties properties;
   
   public NodeProperties(Properties properties) {
      this.properties = properties;
   }
   
   public long getLastSyncMail() {
      long time = 0;
      try { 
         time = Long.parseLong(properties.getProperty(LAST_SYNC_MAIL, "0"));
      } catch(Throwable e) {
         LOG.error("Unable to retrieve "+LAST_SYNC_MAIL+" property, using defaults");
      }
      return time;
   }
   
   public long getLastSyncChat() {
      long time = 0;
      try { 
         time = Long.parseLong(properties.getProperty(LAST_SYNC_CHAT, "0"));
      } catch(Throwable e) {
         LOG.error("Unable to retrieve "+LAST_SYNC_CHAT+" property, using defaults");
      }
      return time;
   }
   
   public long getLastSyncAPRS() {
      long time = 0;
      try { 
         time = Long.parseLong(properties.getProperty(LAST_SYNC_APRS, "0"));
      } catch(Throwable e) {
         LOG.error("Unable to retrieve "+LAST_SYNC_APRS+" property, using defaults");
      }
      return time;
   }
   
   public long getLastSyncNews() {
      long time = 0;
      try { 
         time = Long.parseLong(properties.getProperty(LAST_SYNC_NEWS, "0"));
      } catch(Throwable e) {
         LOG.error("Unable to retrieve "+LAST_SYNC_NEWS+" property, using defaults");
      }
      return time;
   }
   
   public void setLastSyncMail(long time) {
      properties.setProperty(LAST_SYNC_MAIL, Long.toString(time));
   }
   
   public void setLastSyncChat(long time) {
      properties.setProperty(LAST_SYNC_CHAT, Long.toString(time));
   }
   
   public void setLastSyncAPRS(long time) {
      properties.setProperty(LAST_SYNC_APRS, Long.toString(time));
   }
   
   public void setLastSyncNews(long time) {
      properties.setProperty(LAST_SYNC_NEWS, Long.toString(time));
   }

   Properties getProperties() {
      return properties;
   }
   
   
   
}
