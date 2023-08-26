package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.node.connectivity.ax25.Interface;
import org.prowl.distribbs.node.connectivity.ax25.InterfaceStatus;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;

import java.io.IOException;

/**
 * Change the current interface (like port on a kantronics)
 */
@BBSCommand
public class Interfaces extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }


        // No parameter? Just list the interfaces then
        if (data.length == 1) {
            showInterfaces();
            return true;
        }
        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"interface", "int", "port"};
    }


    public void showInterfaces() throws IOException {
        write(CR + ANSI.BOLD + ANSI.UNDERLINE + "No. State Interface                                      " + ANSI.NORMAL + CR);
        int i = 0;
        for (Interface anInterface : DistriBBS.INSTANCE.getInterfaceHandler().getInterfaces()) {
            InterfaceStatus interfaceStatus = anInterface.getInterfaceStatus();
            String statusCol;
            if (interfaceStatus.getState() == InterfaceStatus.State.OK) {
                statusCol = ANSI.GREEN;
            } else if (interfaceStatus.getState() == InterfaceStatus.State.WARN) {
                statusCol = ANSI.YELLOW;
            } else if (interfaceStatus.getState() == InterfaceStatus.State.ERROR) {
                statusCol = ANSI.RED;
            } else {
                statusCol = ANSI.WHITE;
            }
            write(StringUtils.rightPad(Integer.toString(i) + ": ", 4) + statusCol + StringUtils.rightPad(interfaceStatus.getState().name(), 6) + ANSI.NORMAL + anInterface.toString() + CR);
            if (interfaceStatus.getMessage() != null) {
                write("      " + statusCol + "\\-" + interfaceStatus.getMessage() + ANSI.NORMAL + CR);
            }
            i++;
        }
        write(CR);
    }
}
