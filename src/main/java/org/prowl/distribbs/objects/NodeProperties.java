package org.prowl.distribbs.objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

public class NodeProperties {

    public static final String LAST_SYNC_MAIL = "lastSyncMail";
    public static final String LAST_SYNC_NEWS = "lastSyncNews";
    public static final String LAST_SYNC_APRS = "lastSyncAPRS";
    public static final String LAST_SYNC_CHAT = "lastSyncChat";
    private static final Log LOG = LogFactory.getLog("NodeProperties");
    private Properties properties;

    public NodeProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Convenience method to get our first sync 'time'
     *
     * @return a time to sync back to, in millis
     */
    public static final String getInitialSyncTime() {
        return Long.toString(System.currentTimeMillis() - (1000l * 60l * 60l * 24l * 365l * 2l)); // 2 years ago.
    }

    public long getLastSyncMail() {

        long time = 0;
        try {
            time = Long.parseLong(properties.getProperty(LAST_SYNC_MAIL, getInitialSyncTime()));
        } catch (Throwable e) {
            LOG.error("Unable to retrieve " + LAST_SYNC_MAIL + " property, using defaults");
        }
        return time;
    }

    public void setLastSyncMail(long time) {
        properties.setProperty(LAST_SYNC_MAIL, Long.toString(time));
    }

    public long getLastSyncChat() {
        long time = 0;
        try {
            time = Long.parseLong(properties.getProperty(LAST_SYNC_CHAT, getInitialSyncTime()));
        } catch (Throwable e) {
            LOG.error("Unable to retrieve " + LAST_SYNC_CHAT + " property, using defaults");
        }
        return time;
    }

    public void setLastSyncChat(long time) {
        properties.setProperty(LAST_SYNC_CHAT, Long.toString(time));
    }

    public long getLastSyncAPRS() {
        long time = 0;
        try {
            time = Long.parseLong(properties.getProperty(LAST_SYNC_APRS, getInitialSyncTime()));
        } catch (Throwable e) {
            LOG.error("Unable to retrieve " + LAST_SYNC_APRS + " property, using defaults");
        }
        return time;
    }

    public void setLastSyncAPRS(long time) {
        properties.setProperty(LAST_SYNC_APRS, Long.toString(time));
    }

    public long getLastSyncNews() {
        long time = 0;
        try {
            time = Long.parseLong(properties.getProperty(LAST_SYNC_NEWS, getInitialSyncTime()));
        } catch (Throwable e) {
            LOG.error("Unable to retrieve " + LAST_SYNC_NEWS + " property, using defaults");
        }
        return time;
    }

    public void setLastSyncNews(long time) {
        properties.setProperty(LAST_SYNC_NEWS, Long.toString(time));
    }

    Properties getProperties() {
        return properties;
    }

}
