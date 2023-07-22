package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.annotations.Commandable;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.ANSI;

import java.io.IOException;

@Commandable
public class ColourToggle extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        client.setColourEnabled(!client.getColourEnabled());
        if (client.getColourEnabled()) {
            write(Messages.get("colourEnabled") + CR);
        } else {
            write(Messages.get("colourDisabled") + CR);
        }

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"CC"};
    }
}
