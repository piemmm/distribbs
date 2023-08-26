package org.prowl.distribbs.services.console.parser;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang.StringUtils;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.node.connectivity.ax25.Interface;
import org.prowl.distribbs.statistics.types.MHeard;
import org.prowl.distribbs.utils.Tools;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommandParser {

    public static final String PROMPT = ":";
    public static final String UNKNOWN_COMMAND = "Unknown command!";
    public static final String INCORRECT_ARGS = "Incorrect number of arguments for command!";
    public static final String INVALID_ARGUMENT = "Invalid data supplied for command!";
    public static final String PORT_UNSUPPORTED = "Operation unsupported on this port";

    private ScreenWriter screen;

    private Mode mode = Mode.CMD;                                    // Default to command mode.
    private int port = 0;                                           // Default radio port
    private MonitorLevel monitorLevel = MonitorLevel.NONE;                           // Level of monitoring

    public CommandParser(ScreenWriter screen) {
        this.screen = screen;
        ServerBus.INSTANCE.register(this);
    }

    public void parse(String c) {

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
            screen.write(getPrompt());
        }
    }

    /**
     * Do the command.
     *
     * @param command
     * @param arguments
     */
    public void doCommand(Command command, String[] arguments) {
        switch (command) {
            case HELP:
                showHelp(arguments);
                break;

            case PORT:
                changePort(arguments);
                break;
            case PORTS:
                showPorts();
                break;
            case HEARD:
                showHeard();
                break;
            case MON:
            case MONITOR:
                monitor(arguments);
                break;
            case BYE:
            case END:
            case LOGOFF:
            case LOGOUT:
            case EXIT:
            case QUIT:
                logout();
                break;

            default:
                unknownCommand();
        }
    }


    public void logout() {
        screen.terminate();
    }

    public void monitor(String[] arguments) {
        if (arguments.length != 2) {
            write(INCORRECT_ARGS);
            return;
        }

        MonitorLevel newLevel = MonitorLevel.findByName(arguments[1]);

        if (newLevel == null) {
            write(INVALID_ARGUMENT);
            return;
        }

        write("Monitor level changed to: " + newLevel.name());
        monitorLevel = newLevel;
    }

    public void showHelp(String[] arguments) {
        screen.write("No help yet");
        // screen.write(getFile("help.txt"));
    }

    public void showPorts() {

        write("List of ports:");

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(3);

        NumberFormat nfb = NumberFormat.getInstance();
        nfb.setMaximumFractionDigits(1);
        nfb.setMinimumFractionDigits(1);

        List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getInterfaces();
        int port = 0;
        write("Port  Driver       RF      Frequency    Noise Floor    Compress(tx/rx)");
        write("----------------------------------------------------------------------");
        for (Interface connector : connectors) {
// todo
//            String noiseFloor = "";
//            if (connector.isRF()) {
//                noiseFloor = "-" + nfb.format(connector.getNoiseFloor()) + " dBm";
//            }
//
//            String freq = "";
//            if (connector.isRF()) {
//                freq = nf.format(connector.getFrequency() / 1000000d);
//            }
//
//            String compressRatio = "-";
//            if (connector.getRxUncompressedByteCount() + connector.getTxCompressedByteCount() != 0) {
//                compressRatio = nf.format(((double) connector.getTxUncompressedByteCount() / (double) connector.getTxCompressedByteCount())) + "/" + nf.format(((double) connector.getRxUncompressedByteCount() / (double) connector.getRxCompressedByteCount()));
//            }

            //   write(port + ":    " + StringUtils.rightPad(connector.getName(), 13) + StringUtils.rightPad(connector.isRF() ? "yes" : "no", 8) + StringUtils.rightPad(freq, 13) + StringUtils.rightPad(noiseFloor, 11) + compressRatio);
            port++;
        }
    }

    public void showHeard() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        MHeard heard = DistriBBS.INSTANCE.getStatistics().getHeard();
        List<Node> nodes = heard.listHeard();
        if (nodes.size() == 0) {
            write("No nodes heard");
        } else {
            write("Callsign       Last Heard               RSSI");
            write("--------------------------------------------");
            for (Node node : nodes) {
                write(StringUtils.rightPad(node.getCallsign(), 15) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 24) + StringUtils.rightPad("-" + node.getRSSI() + " dBm", 10));
            }
        }
    }

    /**
     * Change the port number
     *
     * @param arguments
     */
    public void changePort(String[] arguments) {
        if (arguments.length == 2) {
            try {
                int p = Integer.parseInt(arguments[1]);
                if (DistriBBS.INSTANCE.getInterfaceHandler().getInterface(p) != null) {
                    port = p;
                    write("Port changed to: " + port);
                } else {
                    write("No such port: " + p);
                }
            } catch (Throwable e) {
                write(INVALID_ARGUMENT);
            }
        } else {
            write(INCORRECT_ARGS);
        }
    }


    public void unknownCommand() {
        screen.write(UNKNOWN_COMMAND);
    }

    public String getPrompt() {
        return mode.name().toLowerCase() + PROMPT;
    }

    public void write(String s) {
        screen.write(s);
    }

    @Subscribe
    public void listen(RxRFPacket packet) {
        String extra = "";
        if (packet.isCorrupt()) {
            extra = "(CRC INCORRECT)";
        }
        switch (monitorLevel) {
            case ALL:
                write("Rx>" + extra + Tools.textOnly(packet.getPacket()));
                break;
            case ANNOUNCE:
                if (packet.getCommand() == PacketTools.ANNOUNCE) {
                    write("Rx>" + extra + Tools.textOnly(packet.getPacket()));
                }
                break;
            case APRS:
                if (packet.getDestination().equals(PacketTools.APRS)) {
                    write("Tx>" + Tools.textOnly(packet.getPacket()));
                }
                break;
            case NONE:
                // Nothing
        }
    }

    @Subscribe
    public void listen(TxRFPacket packet) {
        switch (monitorLevel) {
            case ALL:
                write("Tx>" + Tools.textOnly(packet.getPacket()));
                break;
            case ANNOUNCE:
                if (packet.getCommand() == PacketTools.ANNOUNCE) {
                    write("Tx>" + Tools.textOnly(packet.getPacket()));
                }
                break;
            case APRS:
                if (packet.getDestination().equals(PacketTools.APRS)) {
                    write("Tx>" + Tools.textOnly(packet.getPacket()));
                }
                break;
            case NONE:
                // Nothing
        }
    }


    public void stop() {
        ServerBus.INSTANCE.unregister(this);
    }
}