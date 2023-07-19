package org.prowl.distribbs.uiremote.text.parser;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang.StringUtils;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.core.*;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.node.connectivity.RFConnector;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.statistics.types.MHeard;
import org.prowl.distribbs.uilocal.ansi.parser.Mode;
import org.prowl.distribbs.uilocal.ansi.parser.MonitorLevel;
import org.prowl.distribbs.uilocal.ansi.parser.ScreenWriter;
import org.prowl.distribbs.uiremote.text.TextClient;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandParser {

   public static final String PROMPT           = ">";
   public static final String UNKNOWN_COMMAND  = "Unknown command!";
   public static final String INCORRECT_ARGS   = "Incorrect number of arguments for command!";
   public static final String INVALID_ARGUMENT = "Invalid data supplied for command!";
   public static final String PORT_UNSUPPORTED = "Operation unsupported on this port";

   private static final String CR = "\r";


   private TextClient client;

   private Storage storage;

   private Mode mode             = Mode.CMD;                                    // Default to command mode.

   public CommandParser(TextClient client) {
      this.client = client;
      storage = DistriBBS.INSTANCE.getStorage();

   }


   public void parse(String c) throws IOException {

      if (mode == Mode.CMD) {
         String[] arguments = c.split(" ");

         if (c.length() > 0) {
            Command command = Command.findByName(arguments[0].toUpperCase(Locale.ENGLISH));
            if (command != null) {
               doCommand(command, arguments);
            } else {
               unknownCommand();
            }

         }

        sendPrompt();
      }
   }

   public void sendPrompt() throws IOException {
      try {
         client.send(CR+getPrompt());
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
         case LIST:
         case L:
            listMessages();
            break;
         case CC:
            colourToggle();
            break;
         default:
            unknownCommand();
      }
   }

   public void listMessages() throws IOException {

      SimpleDateFormat sdf = new SimpleDateFormat("ddMM/hhmm");

      List<NewsMessage> messages = storage.getNewsMessagesInOrder(null);

      if (messages.size() == 0) {
         client.send("No messages in BBS yet"+CR);
      } else {
         client.send("%BOLD%Msg#   TSLD  Size To     @Route  From    Date/Time Subject%NORMAL%"+CR);

         NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(0);
         nf.setMinimumFractionDigits(0);
         nf.setGroupingUsed(false);
         for (NewsMessage message: messages) {

            client.send((StringUtils.rightPad(nf.format(message.getMessageNumber()),6) + // MessageId
                      StringUtils.rightPad("", 6) +  // TSLD
                    StringUtils.leftPad(nf.format(message.getBody().length), 5)+" "+ // Size
                    StringUtils.rightPad(message.getGroup(), 7)+  // To
                    StringUtils.rightPad(message.getRoute(), 8)+ // @route
                    StringUtils.rightPad(message.getFrom(), 8)+ // from
                    StringUtils.rightPad(sdf.format(message.getDate()), 10)+ // date/time
                    StringUtils.rightPad(message.getSubject(),50)).trim()+CR);
         }
      }

   }

   public void colourToggle() throws IOException {
      client.setColourEnabled(!client.getColourEnabled());
      if (client.getColourEnabled()) {
         client.send(Messages.get("colourEnabled") + CR);
      } else {
         client.send(Messages.get("colourDisabled") + CR);
      }
   }
   
//   public void changeBaud(String[] arguments) {
//
//      if (arguments.length != 2) {
//         write(INCORRECT_ARGS);
//         return;
//      }
//
//      int baud = 0;
//      try {
//         baud = Integer.parseInt(arguments[1]);
//      } catch(NumberFormatException e) {
//         write(INVALID_ARGUMENT);
//         return;
//      }
//
//      Connector connector = DistriBBS.INSTANCE.getConnectivity().getPort(port);
//      if (connector instanceof RFConnector) {
//        int b = ((RFConnector) connector).setBaud(baud);
//        write("Baud set to: " +b);
//      } else {
//         write(PORT_UNSUPPORTED);
//         return;
//      }
//   }
//
//   public void changeDeviation(String[] arguments) {
//      if (arguments.length != 2) {
//         write(INCORRECT_ARGS);
//         return;
//      }
//
//      double dev = 0;
//      try {
//         dev = Double.parseDouble(arguments[1]);
//      } catch(NumberFormatException e) {
//         write(INVALID_ARGUMENT);
//         return;
//      }
//
//      Connector connector = DistriBBS.INSTANCE.getConnectivity().getPort(port);
//      if (connector instanceof RFConnector) {
//         double d = ((RFConnector) connector).setDeviation(dev / 1000d);
//         write("Deviation set to: "+d);
//      } else {
//         write(PORT_UNSUPPORTED);
//         return;
//      }
//   }
   
//   public void changeDemodFilter(String[] arguments) {
//      if (arguments.length != 2) {
//         write(INCORRECT_ARGS);
//         return;
//      }
//
//      int dem = 0;
//      try {
//         dem = Integer.parseInt(arguments[1]);
//      } catch(NumberFormatException e) {
//         write(INVALID_ARGUMENT);
//         return;
//      }
//
//      Connector connector = DistriBBS.INSTANCE.getConnectivity().getPort(port);
//      if (connector instanceof RFConnector) {
//        int d = ((RFConnector) connector).setDemodFilter(dem);
//        write("Demod filter set to: " +d);
//      } else {
//         write(PORT_UNSUPPORTED);
//         return;
//      }
//
//   }
   
//   public void changeAFCFilter(String[] arguments) {
//      if (arguments.length != 2) {
//         write(INCORRECT_ARGS);
//         return;
//      }
//
//      int afc = 0;
//      try {
//         afc = Integer.parseInt(arguments[1]);
//      } catch(NumberFormatException e) {
//         write(INVALID_ARGUMENT);
//         return;
//      }
//
//      Connector connector = DistriBBS.INSTANCE.getConnectivity().getPort(port);
//      if (connector instanceof RFConnector) {
//         int a = ((RFConnector) connector).setAFCFilter(afc);
//         write("AFC Filter set to: "+a);
//      } else {
//         write(PORT_UNSUPPORTED);
//         return;
//      }
//   }

   public void logout() throws IOException {
      client.send(Messages.get("userDisconnecting")+CR);
      client.flush();
      client.close();
   }


   public void showHelp(String[] arguments) throws IOException {
      client.send("No help yet"+CR);


   }

   public void showPorts() throws IOException {

      write("List of ports:");
      
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(4);
      nf.setMinimumFractionDigits(3);
      
      NumberFormat nfb = NumberFormat.getInstance();
      nfb.setMaximumFractionDigits(1);
      nfb.setMinimumFractionDigits(1);

      List<Connector> connectors = DistriBBS.INSTANCE.getConnectivity().getPorts();
      int port = 0;
      write("Port  Driver       RF      Frequency    Noise Floor    Compress(tx/rx)");
      write("----------------------------------------------------------------------");

      for (Connector connector : connectors) {

         String noiseFloor = "";
         if (connector.isRF()) {
            noiseFloor = "-" + nfb.format(connector.getNoiseFloor()) + " dBm";
         }
         
         String freq = "";
         if (connector.isRF()) {
            freq = nf.format(connector.getFrequency() / 1000000d); 
         }
         
         String compressRatio = "-";
         if (connector.getRxUncompressedByteCount()+connector.getTxCompressedByteCount() != 0) {
            compressRatio = nf.format(((double)connector.getTxUncompressedByteCount()/(double)connector.getTxCompressedByteCount()))+"/"+nf.format(((double)connector.getRxUncompressedByteCount()/(double)connector.getRxCompressedByteCount()));
         }

         write(port + ":    " + StringUtils.rightPad(connector.getName(), 13) + StringUtils.rightPad(connector.isRF() ? "yes" : "no", 8) +StringUtils.rightPad(freq,  13)+ StringUtils.rightPad(noiseFloor, 11)+compressRatio);
         port++;
      }
   }

   public void showHeard() throws IOException {

      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
      MHeard heard = DistriBBS.INSTANCE.getStatistics().getHeard();
      List<Node> nodes = heard.listHeard();
      if (nodes.size() == 0) {
         write("No nodes heard"+CR);
      } else {
         write("Callsign       Last Heard                    RSSI"+CR);
         write("-------------------------------------------------"+CR);


         for (Node node : nodes) {
            String rssi = "-" + node.getRSSI() + " dBm";
            if (node.getRSSI() == Double.MAX_VALUE) {
               rssi = "-";
            }
            write((StringUtils.rightPad(node.getCallsign(),15) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 25) + StringUtils.leftPad(rssi,9)).trim()+CR);
         }
       }
   }

//   /**
//    * Change the port number
//    *
//    * @param arguments
//    */
//   public void changePort(String[] arguments) {
//      if (arguments.length == 2) {
//         try {
//            int p = Integer.parseInt(arguments[1]);
//            if (DistriBBS.INSTANCE.getConnectivity().getPort(p) != null) {
//               port = p;
//               write("Port changed to: " + port);
//            } else {
//               write("No such port: " + p);
//            }
//         } catch (Throwable e) {
//            write(INVALID_ARGUMENT);
//         }
//      } else {
//         write(INCORRECT_ARGS);
//      }
//   }
//
//   public void pingDevice(String[] arguments) {
//
//      if (arguments.length != 2) {
//         write(INCORRECT_ARGS);
//         return;
//      }
//      if (arguments[1].trim().length() < 2) {
//         write(INVALID_ARGUMENT);
//         return;
//      }
//
//      String callsign = arguments[1].trim().toUpperCase(Locale.ENGLISH);
//      Connector c = DistriBBS.INSTANCE.getConnectivity().getPort(port);
//      PacketEngine p = c.getPacketEngine();
//
//      if (p == null) {
//         write("Command not supported on this port");
//         return;
//      }
//
//      write("Pinging '" + callsign + "' on port "+port+"("+c.getFrequency()+")");
//
//      p.ping(callsign,
//            new ResponseListener() {
//
//               @Override
//               public void response(Response r) {
//                  if (r.getResponseTime() != -1) {
//                     write(r.getFrom() + " responded in " + r.getResponseTime() + "ms");
//                  } else {
//                     write("No ping response from " + r.getFrom());
//                  }
//               }
//            });
//   }

   public void unknownCommand() throws IOException {
      client.send(UNKNOWN_COMMAND+CR);
   }

   public String getPrompt() {

      String name = mode.name().toLowerCase();
      if (mode == Mode.CMD) {
         name = DistriBBS.INSTANCE.getMyCall();
      }
      return ANSI.BOLD+name+ANSI.NORMAL + PROMPT;
   }

   public void write(String s) throws IOException {
      client.send(s);
   }

   public void stop() {
      ServerBus.INSTANCE.unregister(this);
   }
}