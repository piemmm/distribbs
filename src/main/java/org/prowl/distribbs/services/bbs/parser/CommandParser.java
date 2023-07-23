package org.prowl.distribbs.services.bbs.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.Messages;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.objects.Storage;
import org.prowl.distribbs.services.bbs.BBSClientHandler;
import org.prowl.distribbs.services.bbs.parser.commands.Command;
import org.prowl.distribbs.utils.ANSI;
import org.prowl.distribbs.utils.UnTokenize;
import org.reflections.Reflections;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public class CommandParser {
    private static final Log LOG = LogFactory.getLog("CommandParser");

    // The end character for the BBS prompt
    public static final String PROMPT = ">";

    // Carriage return
    public static final String CR = "\r";

    // Commands that are available
    private static final Set<Class<?>> ALL_COMMANDS = new Reflections("org.prowl.distribbs.services.bbs.parser.commands").getTypesAnnotatedWith(BBSCommand.class);

    // Command classes that help keep this class cleaner
    private final List<Command> commands = new ArrayList<>();

    // Client we are parsing for
    private final BBSClientHandler client;

    // Default to command mode.
    private Mode mode = Mode.CMD;

    // Stack of modes so we can go back to the previous mode from any command.
    protected List<Mode> modeStack = new ArrayList<>();

    public CommandParser(BBSClientHandler client) {
        this.client = client;
        // Storage for messages
        Storage storage = DistriBBS.INSTANCE.getStorage();
        makeCommands();
    }

    /**
     * Instantiate all the available commands in the CommandItem enum - this lets others add commands as external jars
     * without having to modify this class by using the @Commandable annotation in their command class.
     *
     * @throws IOException
     */
    public void makeCommands() {
        for (Class<?> cl : ALL_COMMANDS) {
            try {
                Object instance = cl.getDeclaredConstructor(new Class[0]).newInstance();
                if (instance instanceof Command) {
                    // Setup the command
                    Command command = (Command) instance;
                    command.setClient(client, this);
                    commands.add(command);
                } else {
                    // This isn't a commandable class!
                    LOG.fatal("Class is not a command: " + cl);
                    System.exit(1);
                }
            } catch (Exception e) {
                LOG.error("Unable to instantiate command: " + cl, e);
            }
        }
    }

    public void parse(String c) throws IOException {
        if (mode == Mode.CMD || mode == Mode.MESSAGE_LIST_PAGINATION || mode == Mode.MESSAGE_READ_PAGINATION) {
            String[] arguments = c.split(" "); // Arguments[0] is the command used.

            // If the command matches, then we will send the command. It is up to the command to check the mode we are
            // in and act accordingly.
            boolean commandExecuted = false;
            for (Command command: commands) {
                String[] supportedCommands = command.getCommandNames();
                for (String supportedCommand: supportedCommands) {
                    if (supportedCommand.equalsIgnoreCase(arguments[0])) {
                        commandExecuted = command.doCommand(arguments) | commandExecuted;
                        // Stop when we executed a command.
                        if (commandExecuted) {
                            break;
                        }
                    }
                }
            }
            if (!commandExecuted && arguments[0].length() > 0) {
                unknownCommand();
            }
            sendPrompt();
        }
    }

    public void sendPrompt() throws IOException {
        try {
            write(CR + getPrompt());
            client.flush();
        } catch (EOFException e) {
            // Connection has gone
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void unknownCommand() throws IOException {
        client.send(CR + ANSI.BOLD_RED + Messages.get("unknownCommand") + ANSI.NORMAL + CR);
    }

    public String getPrompt() {
        String name = Messages.get(mode.toString().toLowerCase());
        return ANSI.BOLD_YELLOW + UnTokenize.str(name) + ANSI.BOLD_WHITE + PROMPT + ANSI.NORMAL + " ";
    }

    /**
     * Convenience method to write and not detokenize a string
     *
     * @param s
     * @throws IOException
     */
    public void write(String s) throws IOException {
        client.send(s);
    }

    /**
     * Convenience method to write a detokenized string
     *
     * @param s
     * @throws IOException
     */
    public void unTokenizeWrite(String s) throws IOException {
        client.send(UnTokenize.str(s));
    }

    public void stop() {
        ServerBus.INSTANCE.unregister(this);
    }

    public void pushModeToStack(Mode mode) {
        modeStack.add(mode);
    }

    public Mode popModeFromStack() {
        if (modeStack.size() > 0) {
            mode = modeStack.get(modeStack.size() - 1);
            modeStack.remove(modeStack.size() - 1);
        } else {
            mode = Mode.CMD;
        }
        return mode;
    }
}