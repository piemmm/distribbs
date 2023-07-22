package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.distribbs.services.bbs.BBSClientHandler;
import org.prowl.distribbs.services.bbs.parser.CommandParser;
import org.prowl.distribbs.services.bbs.parser.Mode;

import java.io.IOException;

public abstract class Command {

    public static final String CR = CommandParser.CR;

    protected BBSClientHandler client;
    protected CommandParser commandParser;

    public Command() {

    }

    public void setClient(BBSClientHandler client, CommandParser commandParser) {
        this.client = client;
        this.commandParser = commandParser;
    }

    /**
     * Execute the command
     * @param data
     * @return true if the command was consumed by this class, false if otherwise.
     * @throws IOException
     */
    public abstract boolean doCommand(String[] data) throws IOException;

    /**
     * This is the command and it's aliases.
     * @return The command and it's aliases.
     */
    public abstract String[] getCommandNames();

    /**
     * Convenience method to write to the client (no detokenisation of strings)
     * @param s
     * @throws IOException
     */
    public void write(String s) throws IOException {
        client.send(s);
    }

    public void setMode(Mode mode) {
        commandParser.setMode(mode);
    }

    public Mode getMode() {
        return commandParser.getMode();
    }
}
