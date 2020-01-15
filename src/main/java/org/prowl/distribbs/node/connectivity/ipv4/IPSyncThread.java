package org.prowl.distribbs.node.connectivity.ipv4;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.NewAPRSMessageEvent;
import org.prowl.distribbs.eventbus.events.NewChatMessageEvent;
import org.prowl.distribbs.eventbus.events.NewMailMessageEvent;
import org.prowl.distribbs.eventbus.events.NewNewsMessageEvent;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.services.InvalidMessageException;
import org.prowl.distribbs.services.Packetable;
import org.prowl.distribbs.services.Priority;
import org.prowl.distribbs.services.aprs.APRSMessage;
import org.prowl.distribbs.services.chat.ChatMessage;
import org.prowl.distribbs.services.messages.MailMessage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.utils.compression.CompressedBlockInputStream;
import org.prowl.distribbs.utils.compression.CompressedBlockOutputStream;

import com.google.common.eventbus.Subscribe;

/**
 * The sync thread is responsible for negotiating a sync of messages between 2
 * clients, as well as propagating things like APRS and chat messages throughout
 * the node network
 */
public class IPSyncThread extends Thread {

   private static final Log    LOG          = LogFactory.getLog("IPSyncThread");
   private static final Object SEND_MONITOR = new Object();

   private Socket              socket;
   private IPv4                ipv4;
   private DataOutputStream    outStream;
   private DataInputStream     inStream;
   private Semaphore           semaphore;
   private Storage             storage;
   private boolean             stop;
   private TxMachine           txMachine;

   /**
    * True if I initiated this connection, false if something connected to me.
    */
   private boolean             isInitiator  = false;

   // comms words
   public static final String  HELLO        = "HELLO";
   public static final String  MAIL_MESSAGE = "MAIL_MESSAGE";
   public static final String  NEWS_MESSAGE = "NEWS_MESSAGE";
   public static final String  CHAT_MESSAGE = "CHAT_MESSAGE";
   public static final String  APRS_MESSAGE = "APRS_MESSAGE";

   public IPSyncThread(Socket socket, IPv4 ipv4, boolean isInitiator) {
      this.socket = socket;
      this.ipv4 = ipv4;
      this.isInitiator = isInitiator;
      semaphore = new Semaphore(1, true);
      storage = DistriBBS.INSTANCE.getStorage();
   }

   public void run() {

      try {
         // Setup our simple cipher - even though over radio we cannot encrypt, we should
         // still make a reasonable attempt at a simple crypt for anything that goes over
         // the Internet
         KeyGenerator encKeyGenerator = KeyGenerator.getInstance("AES");
         KeyGenerator decKeyGenerator = KeyGenerator.getInstance("AES");
         encKeyGenerator.init(128, new SecureRandom(ipv4.getPeerSecret().getBytes()));
         decKeyGenerator.init(128, new SecureRandom(ipv4.getPeerSecret().getBytes()));
         SecretKey encKey = encKeyGenerator.generateKey();
         SecretKey decKey = decKeyGenerator.generateKey();
         Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         encryptCipher.init(Cipher.ENCRYPT_MODE, encKey);
         decryptCipher.init(Cipher.DECRYPT_MODE, decKey);

         try (DataInputStream in = new DataInputStream(new CompressedBlockInputStream(new CipherInputStream(new BufferedInputStream(socket.getInputStream()), decryptCipher)));
               DataOutputStream out = new DataOutputStream(new CompressedBlockOutputStream(new CipherOutputStream(new BufferedOutputStream(socket.getOutputStream()), encryptCipher), 1024))) {

            // Socket options
            socket.setKeepAlive(true); // Keep active connection
            socket.setTcpNoDelay(false); // Small delays acceptable
            socket.setSoTimeout(30000); // 30 second timeout.

            // Whoever initiated the connection has to send the auth packets.
            if (isInitiator) {
               // Write out our hello
               out.writeUTF(HELLO);
               out.flush();
               String reply = in.readUTF();
               if (reply.equals(HELLO)) {
                  beginSync(in, out); // Enter sync state machine.
               }

            } else {
               // Read our hello. If the encryption matches then it should be the same.
               String hello = in.readUTF(); // Allowed to read timeout, if it does then it won't match our hello and we
               // will close the socket
               if (HELLO.equals(hello)) {
                  socket.setSoTimeout(300000); // Now set timeouts to 5 minutes
                  out.writeUTF(HELLO);
                  out.flush();
                  beginSync(in, out); // Enter sync state machine.
               }

            }
         } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
         }

      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }

      inStream = null;
      outStream = null;

      // Close the socket - we're done.
      try {
         socket.close();
      } catch (Throwable e) {
      }
   }

   /**
    * The client has authenticated enough for us to trust it. So we begin a sync
    * 
    * @param in
    * @param out
    */
   private void beginSync(DataInputStream in, DataOutputStream out) throws IOException {
      outStream = out;
      inStream = in;

      // Socket writes are done in a seperate thread and queued.
      txMachine = new TxMachine();
      txMachine.start();

      // Received requests are dealt with here.
      while (true) {
         try {
            String type = in.readUTF();
            if (type.equals(MAIL_MESSAGE)) {

               MailMessage message = new MailMessage().fromPacket(in);
               // If the message does not exist here, then it's new, so save it and post an
               // event so other nodes get passed it.
               if (!storage.doesMailMessageExist(message)) {
                  storage.storeMailMessage(message);
                  ServerBus.INSTANCE.post(new NewMailMessageEvent(message));
               } else {
                  LOG.debug("Already have mail message:" + message);
               }

            } else if (type.equals(CHAT_MESSAGE)) {

               ChatMessage message = new ChatMessage().fromPacket(in);
               if (!storage.doesChatMessageExist(message)) {
                  storage.storeChatMessage(message);
                  ServerBus.INSTANCE.post(new NewChatMessageEvent(message));
               } else {
                  LOG.debug("Already have chat message:" + message);
               }

            } else if (type.equals(NEWS_MESSAGE)) {

               NewsMessage message = new NewsMessage().fromPacket(in);
               if (!storage.doesNewsMessageExist(message)) {
                  storage.storeNewsMessage(message);
                  ServerBus.INSTANCE.post(new NewNewsMessageEvent(message));
               } else {
                  LOG.debug("Already have news message:" + message);
               }

            } else if (type.equals(APRS_MESSAGE)) {

               APRSMessage message = new APRSMessage().fromPacket(in);
               if (!storage.doesAPRSMessageExist(message)) {
                  storage.storeAPRSMessage(message);
                  ServerBus.INSTANCE.post(new NewAPRSMessageEvent(message));
               } else {
                  LOG.debug("Already have APRS message:" + message);
               }

            }
         } catch (InvalidMessageException e) {
            LOG.error(e.getMessage(), e);
         }
      }
   }

   @Subscribe
   public void newNewsEvent(NewNewsMessageEvent event) {
      if (txMachine != null) {
         txMachine.send(NEWS_MESSAGE, event.getMessage());
      }
   }

   @Subscribe
   public void newMailEvent(NewMailMessageEvent event) {
      if (txMachine != null) {
         txMachine.send(MAIL_MESSAGE, event.getMessage());
      }
   }

   @Subscribe
   public void newChatEvent(NewChatMessageEvent event) {
      if (txMachine != null) {
         txMachine.send(CHAT_MESSAGE, event.getMessage());
      }
   }

   @Subscribe
   public void newAPRSEvent(NewAPRSMessageEvent event) {
      if (txMachine != null) {
         txMachine.send(APRS_MESSAGE, event.getMessage());
      }
   }

   /**
    * Send a block of data
    */
   public void write(String type, byte[] data) throws IOException {
      try {
         semaphore.acquire();
      } catch (InterruptedException e) {
         throw new IOException("Could not aquire lock to transmit data");
      }

      try {
         outStream.writeUTF(type);
         outStream.write(data);
         outStream.flush();
      } finally {
         semaphore.release();
      }

   }

   /**
    * The tx machine is a really simple buffer in a seperate thread so that we can
    * queue data and not bother with blocking too much.
    */
   public class TxMachine extends Thread {

      private LinkedList<String> typeToSend;
      private LinkedList<byte[]> dataToSend;

      public TxMachine() {
         dataToSend = new LinkedList<>();
         typeToSend = new LinkedList<>();
      }

      public void run() {

         while (!stop) {
            try {
               if (dataToSend.size() > 0 && typeToSend.size() > 0) {
                  write(typeToSend.removeFirst(), dataToSend.removeFirst());
               } else {
                  try {
                     SEND_MONITOR.wait(1000);
                  } catch (InterruptedException e) {
                  }
               }
            } catch (Throwable e) {
               LOG.error(e.getMessage(), e);
               try {
                  Thread.sleep(100);
               } catch (Throwable ex) {
               }
            }

         }
      }

      private void addData(String type, byte[] data) {
         synchronized (SEND_MONITOR) {
            typeToSend.add(type);
            dataToSend.add(data);
            SEND_MONITOR.notifyAll();
         }
      }

      private void addPriorityData(String type, byte[] data) {
         synchronized (SEND_MONITOR) {
            typeToSend.addFirst(type);
            dataToSend.addFirst(data);
            SEND_MONITOR.notifyAll();
         }
      }

      public void send(String type, Packetable message) {
         if (message.getPriority() == Priority.HIGH) {
            addPriorityData(type, message.toPacket());
         } else {
            addData(type, message.toPacket());
         }
      }

   }

}
