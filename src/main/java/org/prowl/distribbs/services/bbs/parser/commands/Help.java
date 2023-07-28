package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.distribbs.Messages;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.services.bbs.parser.Mode;

import java.io.IOException;

/**
 * Help for commands in CMD mode only
 */
@BBSCommand
public class Help extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);
        write(Messages.get("help") + CR);
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"help", "?", "h"};
    }
}
