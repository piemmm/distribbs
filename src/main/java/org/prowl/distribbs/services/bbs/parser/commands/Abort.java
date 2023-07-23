package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;

import java.io.IOException;

@BBSCommand
public class Abort extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (getMode().equals(Mode.MESSAGE_LIST_PAGINATION) || getMode().equals(Mode.MESSAGE_READ_PAGINATION)) {
            write(ANSI.BOLD + Messages.get("abortMessageList") + ANSI.NORMAL + CR);
            popModeFromStack();
            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"abort", "a"};
    }
}
