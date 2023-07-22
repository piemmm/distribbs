package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.annotations.Commandable;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.statistics.types.MHeard;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Commandable
public class UnHeard extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        write(CR);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

        MHeard heard = DistriBBS.INSTANCE.getStatistics().getUnHeard();
        List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getPorts();
        List<Node> nodes = heard.listHeard();
        if (nodes.size() == 0) {
            write("No nearby nodes unheard yet" + CR);
        } else {
            write(ANSI.UNDERLINE + ANSI.BOLD + "Int  Callsign  Freq/IP      Last UnHeard       CanReach" + ANSI.NORMAL + CR);

            for (Node node : nodes) {
                String rssi = "-" + node.getRSSI() + " dBm";
                if (node.getRSSI() == Double.MAX_VALUE) {
                    rssi = "-  ";
                }

                String freq = Tools.getNiceFrequency(node.getInterface().getFrequency());

                write(StringUtils.rightPad(Integer.toString(connectors.indexOf(node.getInterface())), 5) + StringUtils.rightPad(node.getCallsign(), 10) + StringUtils.rightPad(freq, 13) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 18) + " " + StringUtils.rightPad(canReach(node), 14) + CR);
            }
        }
        return true;
    }


    /**
     * Returns a nice string of callsigns that can reach this node.
     *
     * @param node
     * @return
     */
    public String canReach(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node n : node.getCanReachNodes()) {
            sb.append(n.getCallsign());
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"unheard", "uh"};
    }

}
