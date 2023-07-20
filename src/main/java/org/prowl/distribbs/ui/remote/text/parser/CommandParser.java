package org.prowl.distribbs.ui.remote.text.parser;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.core.*;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.statistics.types.MHeard;

import org.prowl.distribbs.ui.remote.text.TextClient;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;
import org.prowl.distribbs.utils.UnTokenize;

import java.io.EOFException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommandParser {
   private static final Log LOG = LogFactory.getLog("CommandParser");

   public static final String PROMPT           = ">";
   public static final String INCORRECT_ARGS   = "Incorrect number of arguments for command!";
   public static final String INVALID_ARGUMENT = "Invalid data supplied for command!";
   public static final String PORT_UNSUPPORTED = "Operation unsupported on this port";

   // Carriage return
   private static final String CR = "\r";

   // Client we are parsing for
   private TextClient client;
   // Storage for messages
   private Storage storage;
   // Default to command mode.
   private Mode mode             = Mode.CMD;

   // Utility variables related to some commands
   private int listMessagesStartingPoint = 0;                                   // Used for list messages command
   private List<NewsMessage> currentListMessages;                               // Used for list messages command


   public CommandParser(TextClient client) {
      this.client = client;
      storage = DistriBBS.INSTANCE.getStorage();

   }


   public void parse(String c) throws IOException {

      if (mode == Mode.CMD || mode == Mode.MESSAGE_LIST_PAGINATION || mode == Mode.MESSAGE_READ_PAGINATION) {
         String[] arguments = c.split(" ");

        // if (c.length() > 0) {
            Command command = Command.findByName(arguments[0].toUpperCase(Locale.ENGLISH));
            if (command != null) {
               doCommand(command, arguments);
            } else {
               unknownCommand();
            }

        // }

        sendPrompt();
      }
   }

   public void sendPrompt() throws IOException {
      try {
         write(getPrompt());
         client.flush();
      } catch(EOFException e) {
         // Connection has gone
      }
   }

   /**
    * Do the command.
    * 
    * @param command
    * @param arguments
    */
   public void doCommand(Command command, String[] arguments) throws IOException {

      if (mode == Mode.MESSAGE_LIST_PAGINATION) {
         // List messages commands
         switch (command) {
            case A:
            case ABORT:
               write(ANSI.BOLD + Messages.get("abortMessageList") + ANSI.NORMAL + CR);
               mode = Mode.CMD;
               break;
            case R:
            case READ:
               readMessage(arguments);
               break;
            default:
               sendMessageList(currentListMessages);
         }
      } else if (mode == Mode.CMD) {
         // Normal commands
         switch (command) {
            case H:
            case HELP:
               showHelp(arguments);
               break;
            case MH:
            case MHEARD:
            case HEARD:
               showHeard();
               break;
            case B:
            case BYE:
            case END:
            case LOGOFF:
            case LOGOUT:
            case EXIT:
            case QUIT:
               logout();
               break;
            case PORTS:
               showPorts();
               break;
            case LIST:
            case L:
               listMessages(arguments);
               break;
            case CC:
               colourToggle();
               break;
            case ENTER_KEY:
               break;
            default:
               unknownCommand();
         }
      } else {
         LOG.warn("Unknown mode: " + mode);
         unknownCommand();
      }
   }

   public void readMessage(String[] arguments) {

   }

   public void listMessages(String[] arguments) throws IOException {
      listMessagesStartingPoint = 0;
      List<NewsMessage> messages = storage.getNewsMessagesInOrder(null);
      if (messages.size() == 0) {
         write(CR);
         write("No messages in BBS yet"+CR);
      } else {
         write(CR);
         write(ANSI.UNDERLINE+ANSI.BOLD+"Msg#   TSLD  Size To     @Route  From    Date/Time Subject"+ANSI.NORMAL+CR);
         currentListMessages = messages; // Store filtered list for pagination sending
         sendMessageList(currentListMessages);
      }

   }

   public void sendMessageList(List<NewsMessage> messages) throws IOException {
      SimpleDateFormat sdf = new SimpleDateFormat("ddMM/hhmm");
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(0);
      nf.setMinimumFractionDigits(0);
      nf.setGroupingUsed(false);

      int messageSentCounter = 0;
      if (listMessagesStartingPoint != 0) {
         write(CR);
      }
      for (int i = listMessagesStartingPoint; i < messages.size(); i++) {
         NewsMessage message = messages.get(i);

         write((StringUtils.rightPad(nf.format(message.getMessageNumber()),6) + // MessageId
                 StringUtils.rightPad("", 6) +  // TSLD
                 StringUtils.leftPad(nf.format(message.getBody().length), 5)+" "+ // Size
                 StringUtils.rightPad(message.getGroup(), 7)+  // To
                 StringUtils.rightPad(message.getRoute(), 8)+ // @route
                 StringUtils.rightPad(message.getFrom(), 8)+ // from
                 StringUtils.rightPad(sdf.format(message.getDate()), 10)+ // date/time
                 StringUtils.rightPad(message.getSubject(),50)).trim()+CR);

         if (++messageSentCounter >= 10) { // todo '10' should be configurable by the user
            mode = Mode.MESSAGE_LIST_PAGINATION;
            listMessagesStartingPoint = messageSentCounter;
            break;
         }
      }
   }

   public void colourToggle() throws IOException {
      client.setColourEnabled(!client.getColourEnabled());
      if (client.getColourEnabled()) {
         write(Messages.get("colourEnabled") + CR);
      } else {
         write(Messages.get("colourDisabled") + CR);
      }
   }
   

   public void logout() throws IOException {
      client.send(CR);
      client.send(Messages.get("userDisconnecting")+CR);
      client.flush();
      client.close();
   }


   public void showHelp(String[] arguments) throws IOException {
      client.send(CR);
      client.send("No help yet"+CR);
   }

   public void showPorts() throws IOException {
      write(CR);

      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(4);
      nf.setMinimumFractionDigits(3);
      
      NumberFormat nfb = NumberFormat.getInstance();
      nfb.setMaximumFractionDigits(1);
      nfb.setMinimumFractionDigits(1);

      List<Connector> connectors = DistriBBS.INSTANCE.getConnectivity().getPorts();
      int port = 0;
      write(ANSI.UNDERLINE+ANSI.BOLD+"Port  Driver          Freq/IP      Noise Floor  Compress(tx/rx)"+ANSI.NORMAL+CR);

      for (Connector connector : connectors) {

          String noiseFloor = "-";
          if (connector.getNoiseFloor() != Double.MAX_VALUE) {
             noiseFloor = "-" + nfb.format(connector.getNoiseFloor()) + " dBm";
         }

         String freq = Tools.getNiceFrequency(connector.getFrequency());
         String compressRatio = "-";
         if (connector.getRxUncompressedByteCount()+connector.getTxCompressedByteCount() != 0) {
            compressRatio = nf.format(((double)connector.getTxUncompressedByteCount()/(double)connector.getTxCompressedByteCount()))+"/"+nf.format(((double)connector.getRxUncompressedByteCount()/(double)connector.getRxCompressedByteCount()));
         }

         write(port + "     " + StringUtils.rightPad(connector.getName(), 16)  +StringUtils.rightPad(freq,  13)+ StringUtils.rightPad(noiseFloor, 13)+compressRatio+CR);
         port++;
      }
   }

   public void showHeard() throws IOException {
      write(CR);
      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

      MHeard heard = DistriBBS.INSTANCE.getStatistics().getHeard();
      List<Connector> connectors = DistriBBS.INSTANCE.getConnectivity().getPorts();
      List<Node> nodes = heard.listHeard();
      if (nodes.size() == 0) {
         write("No nodes heard"+CR);
      } else {
         write(ANSI.UNDERLINE+ANSI.BOLD+"Port Callsign  Freq/IP      Last Heard             RSSI Capabilities"+ANSI.NORMAL+CR);

         for (Node node : nodes) {
            String rssi = "-" + node.getRSSI() + " dBm";
            if (node.getRSSI() == Double.MAX_VALUE) {
               rssi = "-  ";
            }

            String freq = Tools.getNiceFrequency(node.getConnector().getFrequency());

            write(StringUtils.rightPad(Integer.toString(connectors.indexOf(node.getConnector())),5)+StringUtils.rightPad(node.getCallsign(),10)+StringUtils.rightPad(freq,13) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 18) + StringUtils.leftPad(rssi,9)+" "+StringUtils.rightPad(listCapabilities(node), 14)+CR);
         }
       }
   }

   /**
    * Returns a string list of capability names this node has been seen to perform.
    * @param node
    * @return
    */
   public String listCapabilities(Node node) {
      StringBuilder sb = new StringBuilder();
      for (Capability c: node.getCapabilities()) {
         sb.append(c.getService().getName());
         sb.append(",");
      }
      if (sb.length() > 0) {
         sb.deleteCharAt(sb.length() - 1);
      }
      return sb.toString();
   }

   public void unknownCommand() throws IOException {
      client.send(CR+ANSI.BOLD_RED+Messages.get("unknownCommand")+ANSI.NORMAL+CR);
   }

   public String getPrompt() {
      String name = Messages.get(mode.name().toLowerCase());
      return ANSI.BOLD_YELLOW+ UnTokenize.str(name)+ANSI.BOLD_WHITE+PROMPT+ANSI.NORMAL+" ";
   }


   /**
    * Convenience method to write and not detokenize a string
    * @param s
    * @throws IOException
    */
   public void write(String s) throws IOException {
      client.send(s);
   }


   /**
    * Convenience method to write a detokenized string
    * @param s
    * @throws IOException
    */
   public void unTokenizeWrite(String s) throws IOException {
      client.send(UnTokenize.str(s));
   }

   public void stop() {
      ServerBus.INSTANCE.unregister(this);
   }
}