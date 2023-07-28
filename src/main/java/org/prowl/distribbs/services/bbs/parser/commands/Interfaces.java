package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.node.connectivity.Interface;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.Tools;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;

@BBSCommand
public class Interfaces extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(3);

        NumberFormat nfb = NumberFormat.getInstance();
        nfb.setMaximumFractionDigits(1);
        nfb.setMinimumFractionDigits(1);

        List<Interface> connectors = DistriBBS.INSTANCE.getInterfaceHandler().getPorts();
        int port = 0;
        write(ANSI.UNDERLINE + ANSI.BOLD + "Int   Driver          Freq/IP      Noise Floor  Compress(tx/rx)" + ANSI.NORMAL + CR);

        for (Interface connector : connectors) {

            String noiseFloor = "-";
            if (connector.getNoiseFloor() != Double.MAX_VALUE) {
                noiseFloor = "-" + nfb.format(connector.getNoiseFloor()) + " dBm";
            }

            String freq = Tools.getNiceFrequency(connector.getFrequency());
            String compressRatio = "-";
            if (connector.getRxUncompressedByteCount() + connector.getTxCompressedByteCount() != 0) {
                compressRatio = nf.format(((double) connector.getTxUncompressedByteCount() / (double) connector.getTxCompressedByteCount())) + "/" + nf.format(((double) connector.getRxUncompressedByteCount() / (double) connector.getRxCompressedByteCount()));
            }

            write(port + "     " + StringUtils.rightPad(connector.getName(), 16) + StringUtils.rightPad(freq, 13) + StringUtils.rightPad(noiseFloor, 13) + compressRatio + CR);
            port++;
        }
        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"int", "ports", "i", "interfaces"};
    }
}
