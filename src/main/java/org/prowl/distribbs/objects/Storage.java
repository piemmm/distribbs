package org.prowl.distribbs.objects;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.objects.aprs.APRSMessage;
import org.prowl.distribbs.objects.chat.ChatMessage;
import org.prowl.distribbs.objects.messages.Message;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.utils.Tools;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Storage class allows objects to be stored on disk to keep things simple.
 * <p>
 * Directory based storage, where parts of the object are split up into pieces
 * and recombined when needed as we have plenty of CPU horsepower.
 */
public class Storage {

    private static final Log LOG = LogFactory.getLog("Storage");
    private static final String NEWS = "news";
    private static final String CHAT = "chat";
    private static final String APRS = "aprs";
    private static final String USER = "user";
    private static final String QSL = "qsl";
    // Cache of messages
    private static final Cache<String, Message> BIDMIDToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static final Cache<Long, Message> messageIdToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static final Cache<File, Message> messageFileToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static long highestMessageIdSeen = -1;
    private HierarchicalConfiguration config;
    private File locationDir = new File("storage");


    public Storage(HierarchicalConfiguration config) {
        this.config = config;
    }


    public File getUserMessageFile(String baseCallsign) {
        // Get the location to save the file and make sure the directory structure
        // exists
        String filename = baseCallsign;
        File itemDir = new File(locationDir.getAbsolutePath() + File.separator + USER);
        if (!itemDir.exists()) {
            itemDir.mkdirs();
        }
        return new File(itemDir, filename);
    }


    /**
     * Store a chat message
     * <p>
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
     * <p>
     * Checks local storage to see if a chat message already exists
     */
    public boolean doesChatMessageExist(ChatMessage message) {
        return getChatMessageFile(message).exists();
    }

    /**
     * Store a news message
     * <p>
     * Paths are: module:date:group:messageFile
     *
     * @param message
     */
    public void storeNewsMessage(Message message) throws IOException {
        // Write it to disk
        storeData(getNewsMessageFile(message), message.toPacket());
    }

    /**
     * Write a user to disk.
     *
     * @param user
     * @throws IOException
     */
    public void storeUser(User user) throws IOException {
        storeData(getUserMessageFile(user.getBaseCallsign()), user.toPacket());
    }


    public File getNewsMessageFile(Message message) {
        String filename = message.getBID_MID();
        File itemDir = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator + message.getGroup());
        if (!itemDir.exists()) {
            itemDir.mkdirs();
        }
        return new File(itemDir, filename);
    }

    /**
     * Convenience method
     *
     * @param BID_MID
     * @return
     */
    public File getNewsMessageFile(String BID_MID) {
        Message empty = new Message();
        empty.setBID_MID(BID_MID);
        return getNewsMessageFile(empty);
    }


    /**
     * Convenience method for if a message exists already
     * <p>
     * MIDBID and Group must be populated.
     * <p>
     * Checks local storage to see if a news message already exists
     */
    public boolean doesNewsMessageExist(Message message) {


        return getNewsMessageFile(message).exists();
    }

    /**
     * Store an APRS message
     * <p>
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
     * <p>
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
        String dateStr = Long.toString((int) (timeMillis / 86400000d));
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
    public File[] listMessages(String group) {
        List<File> files = new ArrayList<>();
        if (group != null) {
            // Get for a group
            File groupFile = new File(locationDir.getAbsolutePath() + File.separator + NEWS, group);
            getMessageList(files, groupFile);
        } else {
            // Get all messages
            File[] groups = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator).listFiles();
            for (File fgroup : groups) {
                getMessageList(files, fgroup);
            }
        }

        return files.toArray(new File[files.size()]);
    }

    /**
     * Get a message based on it's messageId
     *
     * @param messageId
     * @return
     */
    public Message getMessage(long messageId) {

        // Check cache first to avoid disk access
        Message message = messageIdToMsg.getIfPresent(messageId);
        if (message != null) {
            return message;
        }

        // If not in cache then retrieve from disk
        List<Message> messages = getMessagesInOrder(null);

        // We could check the cache, but it might have been evicted, so we will check iterate the list anyway
        for (Message msg : messages) {
            if (msg.getMessageNumber() == messageId) {
                message = msg;
                break;
            }
        }

        // Probably re-insert into the cache, just to be sure it wasn't evicted already.
        if (message != null) {
            messageIdToMsg.put(message.getMessageNumber(), message);
        }
        return message;
    }

    /**
     * Get a list of messages for a group and add to the supplied list
     *
     * @param files  the list we will append files found to
     * @param fgroup the group to list files for
     */
    private void getMessageList(List<File> files, File fgroup) {
        try {
            File[] messages = fgroup.listFiles();
            if (messages != null) {
                files.addAll(Arrays.asList(messages));
            }
        } catch (Throwable e) {
            // Ignore the 'not a date' file
            LOG.debug("Ignoring file path:" + fgroup, e);
        }
    }

    /**
     * Get a list of all news group messages going back as far as date X.
     */
    public File[] listMessages() {
        return listMessages(null);
    }
//
//    /**
//     * Get a mail messages for a callsign
//     */
//    public File[] listMailMessages(String callsign, long earliestDate) throws IOException {
//        ArrayList<File> files = new ArrayList<>();
//        File[] groups = new File(locationDir.getAbsolutePath() + File.separator + MAIL + File.separator + callsign).listFiles();
//        if (groups != null) {
//            for (File group : groups) {
//                try {
//                    File[] messages = group.listFiles();
//                    if (messages != null) {
//                        files.addAll(Arrays.asList(messages));
//                    }
//                } catch (NumberFormatException e) {
//                    LOG.debug("Invalid file:" + e.getMessage() + "  " + group);
//                }
//            }
//        }
//        return files.toArray(new File[files.size()]);
//    }
//
//    /**
//     * Get a mail messages since a date
//     */
//    public File[] listMailMessages(long earliestDate) {
//        ArrayList<File> files = new ArrayList<>();
//        File[] callsigns = new File(locationDir.getAbsolutePath() + File.separator + MAIL).listFiles();
//        if (callsigns != null) {
//            for (File callsign : callsigns) {
//                File[] dates = callsign.listFiles();
//                if (dates != null) {
//                    for (File date : dates) {
//                        try {
//                            if (Long.parseLong(date.getName()) > earliestDate) {
//                                File[] messages = date.listFiles();
//                                if (messages != null) {
//                                    files.addAll(Arrays.asList(messages));
//                                }
//                            }
//                        } catch (NumberFormatException e) {
//                            LOG.debug("Invalid file:" + e.getMessage() + "  " + date);
//                        }
//                    }
//                }
//            }
//        }
//        return files.toArray(new File[files.size()]);
//    }

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
     * 'earliestDate'
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
     * 'earliestDate'
     * @throws IOException
     */
    public File[] listChatMessages(long earliestDate) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        File[] groups = new File(locationDir.getAbsolutePath() + File.separator + CHAT).listFiles();
        if (groups != null) {
            for (File chatGroup : groups) {
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
     * <p>
     * Message IDs are immutable, so we can cache the references.
     *
     * @param f
     * @return
     * @throws IOException
     */
    public Message loadNewsMessage(File f) throws IOException {

        Message message = messageFileToMsg.getIfPresent(f);
        if (message != null) {
            return message;
        }

        // Otherwise load the message from disk.
        message = new Message();
        try {
            message.fromPacket(loadData(f));
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }

        // Store in cache for quick lookup.
        BIDMIDToMsg.put(message.getBID_MID(), message);
        messageIdToMsg.put(message.getMessageNumber(), message);
        messageFileToMsg.put(f, message);

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


    public User loadUser(String baseCallsign) throws IOException {
        return loadUserMessage(getUserMessageFile(baseCallsign));
    }

    public User loadUserMessage(File f) throws IOException {
        User user = new User();

        // No user, just return this then
        if (!f.exists()) {
            return user;
        }

        DataInputStream din = loadData(f);
        try {
            user.fromPacket(din);
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }
        return user;
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
     * @param callsign The callsign of the remote node
     */
    public synchronized void saveNodeProperties(String callsign, NodeProperties nodeProperties) {
        try (FileOutputStream fos = new FileOutputStream(getNodePropertiesFile(callsign))) {
            nodeProperties.getProperties().store(fos, "DistriBBS node properties file");
            fos.flush();
        } catch (Throwable e) {
            LOG.error("Unable to save properties file: " + getNodePropertiesFile(callsign));
        }
    }

    /**
     * Get a list of messages in message id order.
     *
     * @param groupName the group to list messages for, or null for all groups
     * @return a list of messages in message id order.
     */
    public List<Message> getMessagesInOrder(String groupName) {
        File[] files = listMessages();
        List<Message> messages = new ArrayList<>();
        for (File file : files) {
            try {
                Message message = loadNewsMessage(file);
                messages.add(message);
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }

        // Sort them in id order
        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                if (o1.getMessageNumber() < o2.getMessageNumber()) {
                    return 1;
                } else if (o1.getMessageNumber() > o2.getMessageNumber()) {
                    return -1;
                }
                return 0;
            }
        });

        return messages;
    }

    /**
     * Scan the news AND mail folder looking for the highest message ID and store in memory and returns
     * the next one.
     */
    public synchronized long getNextMessageID() {
        if (highestMessageIdSeen != -1) {
            return ++highestMessageIdSeen;
        }
        long highest = -1;
        File[] files = listMessages();
        for (File f : files) {
            try {
                Message message = loadNewsMessage(f);
                if (highest < message.getMessageNumber()) {
                    highest = message.getMessageNumber();
                }
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (highest <= 0) {
            highest = 10000; // Starting id
        }

        highestMessageIdSeen = ++highest;
        return highest;
    }


}