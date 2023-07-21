package org.prowl.distribbs.node.connectivity.ipv4;

/**
 * The sync thread is responsible for negotiating a sync of messages between 2
 * clients, as well as propagating things like APRS and chat messages throughout
 * the node network
 */
public class IPSyncThread extends Thread {

//   private static final Log    LOG          = LogFactory.getLog("IPSyncThread");
//   private static final Object SEND_MONITOR = new Object();
//
//   private Socket              socket;
//   private IPv4                ipv4;
//   private DataOutputStream    outStream;
//   private DataInputStream     inStream;
//   private Semaphore           semaphore;
//   private Storage             storage;
//   private boolean             stop;
//   private TxMachine           txMachine;
//   private NodeProperties      nodeProperties;
//   private String              remoteCallsign;
//
//   /**
//    * True if I initiated this connection, false if something connected to me.
//    */
//   private boolean             isInitiator  = false;
//
//   // Start of conversation
//   public static final String  HELLO        = "HELLO";
//
//   // Each message is prefixed with this over ip.
//   public static final String  MAIL_MESSAGE = "MAIL_MESSAGE";
//   public static final String  NEWS_MESSAGE = "NEWS_MESSAGE";
//   public static final String  CHAT_MESSAGE = "CHAT_MESSAGE";
//   public static final String  APRS_MESSAGE = "APRS_MESSAGE";
//
//   // Request a sync starts for the relevant group of messages
//   public static final String  MAIL_FROM    = "SEND_MAIL_FROM";                 // Request sync 'send mail from <time>'
//   public static final String  CHAT_FROM    = "SEND_CHAT_FROM";
//   public static final String  NEWS_FROM    = "SEND_NEWS_FROM";
//   public static final String  APRS_FROM    = "SEND_APRS_FROM";
//
//   // Send the latest time we are synced to so it can be stored
//   // ready for the next sync
//   public static final String  MAIL_LAST    = "SEND_MAIL_LAST";                 // latest mail message sent. 'end of sync <latest_message_time>'
//   public static final String  CHAT_LAST    = "SEND_CHAT_LAST";
//   public static final String  NEWS_LAST    = "SEND_NEWS_LAST";
//   public static final String  APRS_LAST    = "SEND_APRS_LAST";
//
//   public IPSyncThread(Socket socket, IPv4 ipv4, String remoteCallsign, boolean isInitiator) {
//      this.socket = socket;
//      this.ipv4 = ipv4;
//      this.isInitiator = isInitiator;
//      this.remoteCallsign = remoteCallsign;
//      semaphore = new Semaphore(1, true);
//      storage = DistriBBS.INSTANCE.getStorage();
//      nodeProperties = storage.loadNodeProperties(remoteCallsign);
//      ServerBus.INSTANCE.register(this);
//   }
//
//   /**
//    * We close this sync thread down if another one has connected for this callsign
//    * @param e
//    */
//   @Subscribe
//   public void clientConnected(IPNodeConnectedEvent e) {
//      if (e.getCallsign().equals(remoteCallsign)) {
//         if (e.getIpSyncThread() != this) {
//            stop = true;
//            try { socket.close(); } catch(Throwable ex) { }
//         }
//
//      }
//
//   }
//
//   public void run() {
//
//      try {
//         // Setup our simple cipher - even though over radio we cannot encrypt, we should
//         // still make a reasonable attempt at a simple crypt for anything that goes over
//         // the Internet
//         KeyGenerator encKeyGenerator = KeyGenerator.getInstance("AES");
//         KeyGenerator decKeyGenerator = KeyGenerator.getInstance("AES");
//         encKeyGenerator.init(128, new SecureRandom(ipv4.getPeerSecret().getBytes()));
//         decKeyGenerator.init(128, new SecureRandom(ipv4.getPeerSecret().getBytes()));
//         SecretKey encKey = encKeyGenerator.generateKey();
//         SecretKey decKey = decKeyGenerator.generateKey();
//         Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//         Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//         encryptCipher.init(Cipher.ENCRYPT_MODE, encKey);
//         decryptCipher.init(Cipher.DECRYPT_MODE, decKey);
//
//         try (DataInputStream in = new DataInputStream(new CipherInputStream(new BufferedInputStream(socket.getInputStream()), decryptCipher));
//               DataOutputStream out = new DataOutputStream(new CipherOutputStream(new BufferedOutputStream(socket.getOutputStream()), encryptCipher))) {
//
//            // Socket options
//            socket.setKeepAlive(true); // Keep active connection
//            socket.setTcpNoDelay(false); // Small delays acceptable
//            socket.setSoTimeout(30000); // 30 second timeout.
//
//            // Whoever initiated the connection has to send the auth packets.
//            if (isInitiator) {
//               // Write out our hello
//               out.writeUTF(HELLO);
//               out.flush();
//               String reply = in.readUTF();
//               if (reply.equals(HELLO)) {
//                  beginSync(in, out); // Enter sync state machine.
//               }
//
//            } else {
//               // Read our hello. If the encryption matches then it should be the same.
//               String hello = in.readUTF(); // Allowed to read timeout, if it does then it won't match our hello and we
//               // will close the socket
//               if (HELLO.equals(hello)) {
//                  ServerBus.INSTANCE.post(new IPNodeConnectedEvent(remoteCallsign, this));
//                  socket.setSoTimeout(300000); // Now set timeouts to 5 minutes
//                  out.writeUTF(HELLO);
//                  out.flush();
//                  beginSync(in, out); // Enter sync state machine.
//               }
//
//            }
//         } catch (IOException e) {
//            LOG.warn(e.getMessage(), e);
//         }
//
//      } catch (Throwable e) {
//         LOG.error(e.getMessage(), e);
//      } finally {
//         inStream = null;
//         outStream = null;
//         stop = true;
//      }
//      // Close the socket - we're done.
//      try {
//         socket.close();
//      } catch (Throwable e) {
//      }
//      LOG.debug("Thread ending");
//   }
//
//   /**
//    * The client has authenticated enough for us to trust it. So we begin a sync
//    *
//    * @param in
//    * @param out
//    */
//   private void beginSync(DataInputStream in, DataOutputStream out) throws IOException {
//      outStream = out;
//      inStream = in;
//
//      // Socket writes are done in a seperate thread and queued.
//      txMachine = new TxMachine();
//      txMachine.start();
//
//      // Request a sync once connected, and every 12 hours after.
//      Timer timer = new Timer();
//      timer.schedule(new TimerTask() {
//         public void run() {
//            if (stop) {
//               timer.cancel();
//            } else {
//               // Enqueue a catchup of messages
//               requestMailMessages();
//               requestNewsMessages();
//               requestChatMessages();
//               requestAPRSMessages();
//            }
//         }
//      }, 5000l, (1000l * 60l * 60l * 12l));
//
//      // Received requests are dealt with here.
//      while (!stop) {
//         try {
//            String type = in.readUTF();
//            if (type.equals(MAIL_MESSAGE)) {
//
//               MailMessage message = new MailMessage().fromPacket(in);
//               // If the message does not exist here, then it's new, so save it and post an
//               // event so other nodes get passed it.
//               if (!storage.doesMailMessageExist(message)) {
//                  storage.storeMailMessage(message);
//                  ServerBus.INSTANCE.post(new NewMailMessageEvent(message));
//               } else {
//                  LOG.debug("Already have mail message:" + message);
//               }
//
//            } else if (type.equals(CHAT_MESSAGE)) {
//
//               ChatMessage message = new ChatMessage().fromPacket(in);
//               if (!storage.doesChatMessageExist(message)) {
//                  storage.storeChatMessage(message);
//                  ServerBus.INSTANCE.post(new NewChatMessageEvent(message));
//               } else {
//                  LOG.debug("Already have chat message:" + message);
//               }
//
//            } else if (type.equals(NEWS_MESSAGE)) {
//
//               NewsMessage message = new NewsMessage().fromPacket(in);
//               if (!storage.doesNewsMessageExist(message)) {
//                  storage.storeNewsMessage(message);
//                  ServerBus.INSTANCE.post(new NewNewsMessageEvent(message));
//               } else {
//                  LOG.debug("Already have news message:" + message);
//               }
//
//            } else if (type.equals(APRS_MESSAGE)) {
//
//               APRSMessage message = new APRSMessage().fromPacket(in);
//               if (!storage.doesAPRSMessageExist(message)) {
//                  storage.storeAPRSMessage(message);
//                  ServerBus.INSTANCE.post(new NewAPRSMessageEvent(message));
//               } else {
//                  LOG.debug("Already have APRS message:" + message);
//               }
//
//            } else if (type.equals(MAIL_FROM)) {
//               long messagesFrom = in.readLong();
//               sendMailFrom(messagesFrom);
//            } else if (type.equals(NEWS_FROM)) {
//               long newsFrom = in.readLong();
//               sendNewsFrom(newsFrom);
//            } else if (type.equals(CHAT_FROM)) {
//               long chatFrom = in.readLong();
//               sendChatFrom(chatFrom);
//            } else if (type.equals(APRS_FROM)) {
//               long aprsFrom = in.readLong();
//               sendAPRSFrom(aprsFrom);
//            } else if (type.equals(MAIL_LAST)) {
//               // Always sync the last 13 hours because we may have a 'late' message missed.
//               long latest = in.readLong() - (1000l * 60l * 60l * 13l);
//               nodeProperties.setLastSyncMail(latest);
//               storage.saveNodeProperties(remoteCallsign, nodeProperties);
//            } else if (type.equals(CHAT_LAST)) {
//               long latest = in.readLong() - (1000l * 60l * 60l * 13l);
//               ;
//               nodeProperties.setLastSyncChat(latest);
//               storage.saveNodeProperties(remoteCallsign, nodeProperties);
//            } else if (type.equals(NEWS_LAST)) {
//               long latest = in.readLong() - (1000l * 60l * 60l * 13l);
//               ;
//               nodeProperties.setLastSyncNews(latest);
//               storage.saveNodeProperties(remoteCallsign, nodeProperties);
//            } else if (type.equals(MAIL_LAST)) {
//               // ARPS ignored
//            }
//         } catch (InvalidMessageException e) {
//            LOG.error(e.getMessage(), e);
//         }
//      }
//   }
//
//   @Subscribe
//   public void newNewsEvent(NewNewsMessageEvent event) {
//      if (txMachine != null) {
//         txMachine.send(NEWS_MESSAGE, event.getMessage());
//      }
//   }
//
//   @Subscribe
//   public void newMailEvent(NewMailMessageEvent event) {
//      if (txMachine != null) {
//         txMachine.send(MAIL_MESSAGE, event.getMessage());
//      }
//   }
//
//   @Subscribe
//   public void newChatEvent(NewChatMessageEvent event) {
//      if (txMachine != null) {
//         txMachine.send(CHAT_MESSAGE, event.getMessage());
//      }
//   }
//
//   @Subscribe
//   public void newAPRSEvent(NewAPRSMessageEvent event) {
//      if (txMachine != null) {
//         txMachine.send(APRS_MESSAGE, event.getMessage());
//      }
//   }
//
//   /**
//    * Write a block of data to the remote node
//    */
//   public void write(String type, byte[] data) throws IOException {
//      try {
//         semaphore.acquire();
//      } catch (InterruptedException e) {
//         throw new IOException("Could not aquire lock to transmit data");
//      }
//
//      try {
//         outStream.writeUTF(type);
//         outStream.write(data);
//         outStream.flush();
//      } finally {
//         semaphore.release();
//      }
//
//   }
//
//   /**
//    * The tx machine is a really simple buffer in a seperate thread so that we can
//    * queue data and not bother with blocking too much.
//    */
//   public class TxMachine extends Thread {
//
//      private LinkedList<String> typeToSend;
//      private LinkedList<byte[]> dataToSend;
//
//      public TxMachine() {
//         dataToSend = new LinkedList<>();
//         typeToSend = new LinkedList<>();
//      }
//
//      public void run() {
//
//         while (!stop) {
//            try {
//               if (dataToSend.size() > 0 && typeToSend.size() > 0) {
//                  write(typeToSend.removeFirst(), dataToSend.removeFirst());
//               } else {
//                  try {
//                     SEND_MONITOR.wait(1000);
//                  } catch (InterruptedException e) {
//                  }
//               }
//            } catch (Throwable e) {
//               LOG.error(e.getMessage(), e);
//               try {
//                  Thread.sleep(100);
//               } catch (Throwable ex) {
//               }
//            }
//
//         }
//      }
//
//      private void addData(String type, byte[] data) {
//         synchronized (SEND_MONITOR) {
//            typeToSend.add(type);
//            dataToSend.add(data);
//            SEND_MONITOR.notifyAll();
//         }
//      }
//
//      private void addPriorityData(String type, byte[] data) {
//         synchronized (SEND_MONITOR) {
//            typeToSend.addFirst(type);
//            dataToSend.addFirst(data);
//            SEND_MONITOR.notifyAll();
//         }
//      }
//
//      public void send(String type, Packetable message) {
//         if (message.getPriority() == Priority.HIGH) {
//            addPriorityData(type, message.toPacket());
//         } else {
//            addData(type, message.toPacket());
//         }
//      }
//
//   }
//
//   private void requestMailMessages() {
//      // Get the last sync. This can be up to(but no more than) a year ago for a first
//      // time sync
//      long lastSyncTime = nodeProperties.getLastSyncMail();
//      txMachine.addData(MAIL_FROM, Tools.longToByte(lastSyncTime));
//   }
//
//   private void requestNewsMessages() {
//      // Get the last sync. This can be up to(but no more than) a year ago for a first
//      // time sync
//      long lastSyncTime = nodeProperties.getLastSyncNews();
//      try {
//         ByteArrayOutputStream bos = new ByteArrayOutputStream();
//         DataOutputStream dos = new DataOutputStream(bos);
//         dos.writeLong(lastSyncTime);
//         dos.flush();
//         dos.close();
//         txMachine.addData(NEWS_FROM, bos.toByteArray());
//      } catch (IOException e) {
//         // Report error, don't send message
//         LOG.error(e.getMessage(), e);
//      }
//   }
//
//   private void requestChatMessages() {
//      // Get the last sync. This can be up to(but no more than) a year ago for a first
//      // time sync
//      long lastSyncTime = nodeProperties.getLastSyncChat();
//      try {
//         ByteArrayOutputStream bos = new ByteArrayOutputStream();
//         DataOutputStream dos = new DataOutputStream(bos);
//         dos.writeLong(lastSyncTime);
//         dos.flush();
//         dos.close();
//         txMachine.addData(CHAT_FROM, bos.toByteArray());
//      } catch (IOException e) {
//         // Report error, don't send message
//         LOG.error(e.getMessage(), e);
//      }
//   }
//
//   private void requestAPRSMessages() {
//      // Don't do anything here - we won't bother with an APRS 'backlog' we're only
//      // really interested in local 'realtime' messages
//   }
//
//   /**
//    * Send all messages after 'earliestDate' to the requesting node.
//    *
//    * @param earliestDate
//    */
//   private void sendMailFrom(long earliestDate) {
//      try {
//         File[] messages = storage.listMailMessages(earliestDate);
//         long latestMessage = 0;
//         for (File messageFile : messages) {
//            MailMessage m = storage.loadMailMessage(messageFile);
//            if (latestMessage < m.getDate()) {
//               latestMessage = m.getDate();
//            }
//            txMachine.addData(MAIL_MESSAGE, m.toPacket());
//         }
//         txMachine.addData(MAIL_LAST, Tools.longToByte(latestMessage));
//      } catch (IOException e) {
//         // Display error and don't send any messages
//         LOG.error(e.getMessage(), e);
//      }
//   }
//
//   private void sendNewsFrom(long earliestDate) {
//      try {
//         File[] messages = storage.listNewsMessages(earliestDate);
//         long latestMessage = 0;
//         for (File messageFile : messages) {
//            NewsMessage m = storage.loadNewsMessage(messageFile);
//            if (latestMessage < m.getDate()) {
//               latestMessage = m.getDate();
//            }
//            txMachine.addData(NEWS_MESSAGE, m.toPacket());
//         }
//         txMachine.addData(NEWS_LAST, Tools.longToByte(latestMessage));
//
//      } catch (IOException e) {
//         // Display error and don't send any messages
//         LOG.error(e.getMessage(), e);
//      }
//   }
//
//   private void sendChatFrom(long earliestDate) {
//      try {
//         File[] messages = storage.listChatMessages(earliestDate);
//         long latestMessage = 0;
//         for (File messageFile : messages) {
//            ChatMessage m = storage.loadChatMessage(messageFile);
//            if (latestMessage < m.getDate()) {
//               latestMessage = m.getDate();
//            }
//            txMachine.addData(CHAT_MESSAGE, m.toPacket());
//         }
//         txMachine.addData(CHAT_LAST, Tools.longToByte(latestMessage));
//
//      } catch (IOException e) {
//         // Display error and don't send any messages
//         LOG.error(e.getMessage(), e);
//      }
//   }
//
//   private void sendAPRSFrom(long earliestDate) {
//      // We don't do anything here as we're only interested in 'realtime' aprs
//   }
//
//   public void stopNow() {
//      stop = true;
//   }
}
