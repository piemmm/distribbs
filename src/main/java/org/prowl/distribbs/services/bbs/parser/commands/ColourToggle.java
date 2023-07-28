package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.distribbs.Messages;
import org.prowl.distribbs.annotations.BBSCommand;

import java.io.IOException;

@BBSCommand
public class ColourToggle extends Command {

    /**
     * Colour toggle is a special case command and is accessible in any mode.
     *
     * @param data
     * @return
     * @throws IOException
     */
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
