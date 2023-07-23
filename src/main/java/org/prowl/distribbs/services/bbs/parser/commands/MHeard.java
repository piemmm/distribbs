package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Capability;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@BBSCommand
public class MHeard extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

            write(CR);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
        org.prowl.distribbs.statistics.types.MHeard heard = DistriBBS.INSTANCE.getStatistics().getHeard();
        List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getPorts();
        List<Node> nodes = heard.listHeard();
        if (nodes.size() == 0) {
            write("No nodes heard" + CR);
        } else {
            write(ANSI.UNDERLINE + ANSI.BOLD + "Int  Callsign  Freq/IP      Last Heard             RSSI Capabilities" + ANSI.NORMAL + CR);
            for (Node node : nodes) {
                String rssi = "-" + node.getRSSI() + " dBm";
                if (node.getRSSI() == Double.MAX_VALUE) {
                    rssi = "-  ";
                }

                String freq = Tools.getNiceFrequency(node.getInterface().getFrequency());

                write(StringUtils.rightPad(Integer.toString(connectors.indexOf(node.getInterface())), 5) + StringUtils.rightPad(node.getCallsign(), 10) + StringUtils.rightPad(freq, 13) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 18) + StringUtils.leftPad(rssi, 9) + " " + StringUtils.rightPad(listCapabilities(node), 14) + CR);
            }
        }
        return true;
    }


    /**
     * Returns a string list of capability names this node has been seen to perform.
     *
     * @param node
     * @return
     */
    public String listCapabilities(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Capability c : node.getCapabilities()) {
            sb.append(c.getService().getName());
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"mheard", "heard", "mh"};
    }
}
