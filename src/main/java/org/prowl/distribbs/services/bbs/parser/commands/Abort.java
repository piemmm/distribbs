package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.annotations.Commandable;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;

import java.io.IOException;

@Commandable
public class Abort extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (getMode() == Mode.MESSAGE_LIST_PAGINATION || getMode() == Mode.MESSAGE_READ_PAGINATION) {
            write(ANSI.BOLD + Messages.get("abortMessageList") + ANSI.NORMAL + CR);
            setMode(Mode.CMD);
            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"abort", "a"};
    }
}
