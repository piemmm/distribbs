package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.annotations.Commandable;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;

import java.io.IOException;

@Commandable
public class Bye extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        // Possibly save user at this point?

        write(CR);
        write(Messages.get("userDisconnecting") + CR);
        client.flush();
        client.close();

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"b","q","bye","end","logoff","logout","exit","quit"};
    }
}
