package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.annotations.Commandable;
import org.prowl.distribbs.Messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@Commandable
public class Help extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        write(CR);
        write(Messages.get("help") + CR);
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"help", "?", "h"};
    }
}
